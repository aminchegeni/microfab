package ir.co.isc.spbp.blockchain.fabino.unit;

import ir.co.isc.spbp.blockchain.fabino.unit.ledger.LedgerFacade;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.protos.peer.ChaincodeMessage;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.support.FieldContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.util.ExceptionUtils;

import java.lang.reflect.*;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.extension.ExtensionContext.*;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.ModifierSupport.isFinal;
import static org.junit.platform.commons.support.ReflectionSupport.makeAccessible;
import static org.junit.platform.commons.util.ReflectionUtils.isRecordObject;

/**
 * JUnit 5 extension responsible for injecting {@link ChaincodeStub} or
 * {@link Context} instances backed by an in-memory {@link LedgerFacade}.
 * <p>
 * This extension simulates Hyperledger Fabric chaincode invocation semantics
 * by constructing a fully signed {@link ChaincodeMessage} and wiring it to a
 * mock {@link ChaincodeStub}.
 * </p>
 *
 * <h2>Supported Injection Targets</h2>
 * <ul>
 *     <li>Fields annotated with {@link Stub}</li>
 *     <li>Parameters annotated with {@link Stub}</li>
 * </ul>
 *
 * <h2>Ledger Resolution Strategy</h2>
 * <ol>
 *     <li>{@link Namespace#GLOBAL}</li>
 *     <li>Class-level namespace</li>
 *     <li>Test-level namespace</li>
 *     <li>Nearest {@link Ledger} annotation in the context hierarchy</li>
 * </ol>
 *
 * <p>
 * Ledger scoping follows the semantics defined by {@link Ledger.Scope} and
 * ensures deterministic isolation when required.
 * </p>
 *
 * <p>
 * In short: one extension, many stubs, zero regrets.
 * </p>
 */
public class StubExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    /**
     * Store key used for caching the resolved stub or context instance.
     */
    private static final String STUB_KEY = "stub";

    /**
     * Store key used for caching the resolved ledger facade.
     */
    private static final String LEDGER_KEY = "ledger";

    /**
     * Root namespace for all extension-managed stores.
     */
    private static final Namespace NAMESPACE = Namespace.create(StubExtension.class);

    /**
     * Supported injection target types.
     */
    private static final Set<Class<?>> SUPPORTED_TYPES = Set.of(ChaincodeStub.class, Context.class);

    /**
     * Locates and initializes the {@link LedgerFacade} based on the nearest
     * {@link Ledger} annotation found in the extension context hierarchy.
     *
     * @param context the current extension context
     */
    private static void findLedger(ExtensionContext context) {
        LedgerFacade globalLedger = context.getStore(Namespace.GLOBAL).get(LEDGER_KEY, LedgerFacade.class);
        if (nonNull(globalLedger)) {
            return;
        }

        Optional<ExtensionContext> current = Optional.of(context);
        Optional<Ledger> annot = Optional.empty();

        while (current.isPresent()) {
            Optional<Class<?>> testClass = current.get().getTestClass();
            if (testClass.isPresent()) {
                annot = AnnotationSupport.findAnnotation(current.get().getRequiredTestClass(), Ledger.class);
            }
            if (annot.isPresent()) {
                break;
            }
            current = current.get().getParent();
        }

        annot.ifPresent(ledger -> {
            // NOTE: Potential race conditions are acceptable here due to
            // deterministic Store semantics and idempotent initialization.
            if (ledger.scope() == Ledger.Scope.GLOBAL) {
                context.getStore(Namespace.GLOBAL)
                        .put(LEDGER_KEY, LedgerFacade.of(ledger));
            } else if (ledger.scope() == Ledger.Scope.CLASS) {
                context.getStore(Namespace.create(Ledger.Scope.CLASS))
                        .put(LEDGER_KEY, LedgerFacade.of(ledger));
            } else {
                // TEST scope: create a fresh LedgerFacade per execution
                context.getStore(Namespace.create(Ledger.Scope.TEST))
                        .put(LEDGER_KEY, ledger);
            }
        });
    }

    /**
     * Resolves the effective {@link LedgerFacade} using scoped lookup with
     * graceful fallback.
     */
    private static LedgerFacade getLedger(ExtensionContext context, AnnotatedElementContext elementContext) {

        return Optional.ofNullable(
                        context.getStore(Namespace.GLOBAL)
                                .get(LEDGER_KEY, LedgerFacade.class))
                .orElseGet(() ->
                        Optional.ofNullable(
                                        context.getStore(Namespace.create(Ledger.Scope.CLASS))
                                                .get(LEDGER_KEY, LedgerFacade.class))
                                .orElseGet(() ->
                                        Optional.ofNullable(
                                                        context.getStore(Namespace.create(Ledger.Scope.TEST))
                                                                .get(LEDGER_KEY, Ledger.class))
                                                .map(LedgerFacade::of)
                                                .orElseGet(() ->
                                                        elementContext.findAnnotation(Ledger.class)
                                                                .map(LedgerFacade::of)
                                                                // This path is logically unreachable
                                                                .orElse(null))));
    }

    /**
     * Ensures that {@code @Stub}-annotated fields are not declared {@code final}.
     */
    private static void assertNonFinalField(Field field) {
        if (isFinal(field)) {
            throwFinalField(field);
        }
    }

    /**
     * Validates that the injection target type is supported.
     */
    private static void assertSupportedType(String target, Class<?> type) {
        if (SUPPORTED_TYPES.stream().noneMatch(c -> c.isAssignableFrom(type))) {
            throwUnsupportedType(target, type);
        }
    }

    private static void throwFinalField(Field field) {
        throw new ExtensionConfigurationException(
                "@Stub field [%s] must not be declared as final."
                        .formatted(field));
    }

    private static void throwUnsupportedType(String target, Class<?> type) {
        String typeNames = SUPPORTED_TYPES.stream()
                .map(Class::getName)
                .collect(joining(", ", "[", "]"));

        throw new ExtensionConfigurationException(
                "Can only resolve @Stub %s of types %s but was: %s"
                        .formatted(target, typeNames, type.getName()));
    }

    /**
     * Adapts a {@link Field} into a {@link FieldContext} for store scoping.
     */
    private static FieldContext createFieldContext(Field field) {
        return new FieldContext() {
            @Override
            public Field getField() {
                return field;
            }

            @Override
            public int getParameterIndex() {
                return -1;
            }

            @Override
            public AnnotatedElement getAnnotatedElement() {
                return field;
            }
        };
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        findLedger(context);
        injectStaticFields(context, context.getRequiredTestClass());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getRequiredTestInstances()
                .getAllInstances()
                .forEach(instance ->
                        injectInstanceFields(context, instance));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.isAnnotated(Stub.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        assertSupportedType("parameter", parameterType);

        StubInvocationContext invocationContext = determineInvocationContextForParameter(parameterContext);
        Store store = extensionContext.getStore(NAMESPACE.append(parameterContext));
        LedgerFacade ledger = getLedger(extensionContext, parameterContext);

        return getOrCreate(store, ledger, invocationContext, parameterType);
    }

    @Override
    public ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext rootContext) {
        return ExtensionContextScope.TEST_METHOD;
    }

    private void injectStaticFields(ExtensionContext context, Class<?> testClass) {
        injectFields(context, null, testClass, ModifierSupport::isStatic);
    }

    private void injectInstanceFields(ExtensionContext context, Object instance) {
        if (!isRecordObject(instance)) {
            injectFields(context, instance, instance.getClass(), ModifierSupport::isNotStatic);
        }
    }

    private void injectFields(ExtensionContext context, Object instance, Class<?> clazz,
                              Predicate<Field> predicate) {

        findAnnotatedFields(clazz, Stub.class, predicate).forEach(field -> {

            assertNonFinalField(field);
            assertSupportedType("field", field.getType());

            StubInvocationContext invocationContext = determineInvocationContextForField(field);
            FieldContext fieldContext = createFieldContext(field);
            Store store = context.getStore(NAMESPACE.append(fieldContext));
            LedgerFacade ledger = getLedger(context, fieldContext);

            try {
                makeAccessible(field)
                        .set(instance,
                                getOrCreate(store, ledger, invocationContext, field.getType()));
            } catch (Throwable t) {
                throw ExceptionUtils.throwAsUncheckedException(t);
            }
        });
    }

    private StubInvocationContext determineInvocationContextForField(Field field) {
        Stub stub = findAnnotation(field, Stub.class)
                .orElseThrow(() ->
                        new JUnitException(
                                "Field %s must be annotated with @Stub".formatted(field)));

        return determineStubInvocationContext(stub);
    }

    private StubInvocationContext determineInvocationContextForParameter(ParameterContext context) {
        Stub stub = context.findAnnotation(Stub.class)
                .orElseThrow(() ->
                        new JUnitException(
                                "Parameter %s must be annotated with @Stub".formatted(context.getParameter())));

        return determineStubInvocationContext(stub);
    }

    private StubInvocationContext determineStubInvocationContext(Stub stub) {
        return StubInvocationContext.of(stub);
    }

    /**
     * Lazily creates or retrieves a cached {@link ChaincodeStub} or
     * {@link Context} instance for the given invocation context.
     *
     * @param store             invocation-scoped store
     * @param ledger            resolved ledger facade
     * @param invocationContext stub configuration
     * @param clazz             requested injection type
     * @return resolved stub or context instance
     */
    private Object getOrCreate(
            Store store, LedgerFacade ledger, StubInvocationContext invocationContext, Class<?> clazz) {

        return store.getOrComputeIfAbsent(STUB_KEY, __ -> {
            ChaincodeMessage message;
            try {
                message = MessageFactory.of(invocationContext);
            } catch (GeneralSecurityException e) {
                throw new ExtensionConfigurationException(
                        "Error occurred while creating ChaincodeMessage", e);
            }
            ChaincodeStub stub = new ChaincodeStubMock(message, ledger);
            if (ChaincodeStub.class.isAssignableFrom(clazz)) {
                return stub;
            }
            return new Context(stub);
        });
    }
}
