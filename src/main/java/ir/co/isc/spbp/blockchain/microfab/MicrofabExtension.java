package ir.co.isc.spbp.blockchain.microfab;

import io.grpc.*;
import ir.co.isc.spbp.blockchain.microfab.builder.CcBuilder;
import ir.co.isc.spbp.blockchain.microfab.model.Config;
import ir.co.isc.spbp.blockchain.microfab.model.State;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Hash;
import org.hyperledger.fabric.client.identity.*;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ModifierSupport;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ir.co.isc.spbp.blockchain.microfab.utils.Util.toUnchecked;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import static org.junit.jupiter.api.extension.ExtensionContext.Store;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.ReflectionUtils.isRecordObject;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

public class MicrofabExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final Namespace NAMESPACE = Namespace.create(MicrofabExtension.class);
    private static final String CONTAINER_KEY = "microfab.container";
    private static final String CONFIG_KEY = "microfab.config";
    private static final String STATE_KEY = "microfab.state";
    private static final String ADMINS_KEY = "microfab.admins";
    private static final String CONSOLE_KEY = "microfab.console";
    private static final String GATEWAY_KEY = "microfab.gateway";

    private static Optional<Microfab> findMicrofab(ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            Optional<Class<?>> testClass = current.get().getTestClass();
            Optional<Microfab> annot = Optional.empty();
            if (testClass.isPresent()) {
                annot = AnnotationSupport.findAnnotation(current.get().getRequiredTestClass(), Microfab.class);
            }
            if (annot.isPresent()) {
                return annot;
            }
            current = current.get().getParent();
        }
        return Optional.empty();
    }

    private static void buildContainer(ExtensionContext context) {
        Store store = context.getStore(NAMESPACE);
        Config cfg = findMicrofab(context).map(Config::of).orElseGet(Config.builder()::build);
        MicrofabContainer<?> container = new MicrofabContainer<>(cfg);
        container.start();
        State state = State.from(new JSONObject(container.getState()));
        setTlsCertsIfRequired(cfg, state);
        ConsoleClient console = new ConsoleClient(cfg);
        Map<String, State.Identity> admins = console.getAdmins();
        store.put(CONTAINER_KEY, container);
        store.put(CONFIG_KEY, cfg);
        store.put(STATE_KEY, state);
        store.put(ADMINS_KEY, admins);
        store.put(CONSOLE_KEY, console);
    }

    private static void buildChaincode(ExtensionContext context) {
        Store store = context.getStore(NAMESPACE);
        MicrofabContainer<?> container = store.get(CONTAINER_KEY, MicrofabContainer.class);
        Config config = store.getOrDefault(CONFIG_KEY, Config.class, Config.builder().build());
        Map<String, Config.Chaincode> validChaincodes = config.getChaincdoes()
                .stream()
                .collect(Collectors.toMap(Config.Chaincode::getName, Function.identity()));
        config.getChannels()
                .forEach(channel ->
                        channel.getChaincodes()
                                .stream()
                                .filter(validChaincodes::containsKey)
                                .map(validChaincodes::get)
                                .forEach(chaincode -> {
                                    CcBuilder builder = chaincode.getBuilder();
                                    if (builder.detect(chaincode)) {
                                        builder.build(container, channel, chaincode);
                                        builder.run(container, channel, chaincode);
                                    }
                                }));
    }

    private static void setTlsCertsIfRequired(Config config, State state) {
        Config.Tls configuredTls = config.getTls();
        if (configuredTls.isEnabled() && isNull(configuredTls.getPrivateKey())) {
            State.Identity generatedTls = state.getTls();
            configuredTls.setCa(generatedTls.getCa());
            configuredTls.setCertificate(generatedTls.getCertificate());
            configuredTls.setPrivateKey(generatedTls.getPrivateKey());
        }
    }

    private static void injectInstanceFields(ExtensionContext context, Object instance) {
        if (!isRecordObject(instance)) {
            injectFields(context, instance, instance.getClass(), ModifierSupport::isNotStatic);
        }
    }

    private static void injectFields(ExtensionContext context, Object instance, Class<?> clazz, Predicate<Field> filter) {
        findAnnotatedFields(clazz, Msp.class, filter).forEach(field -> {
            assertNonFinalField(field);
            assertSupportedType(field.getType());
            toUnchecked(
                    () -> {
                        Config.Organization org = determineOrg(context, field);
                        Gateway gateway = createGateway(context, org);
                        makeAccessible(field).set(instance, gateway);
                        return null;
                    });
        });
    }

    private static void assertNonFinalField(Field field) {
        if (ModifierSupport.isFinal(field)) {
            throw new ExtensionConfigurationException("@Msp field [" + field + "] must not be declared as final.");
        }
    }

    private static void assertSupportedType(Class<?> type) {
        if (type != Gateway.class) {
            throw new ExtensionConfigurationException(
                    "Can only resolve @Msp field of type %s but was: %s".formatted(Gateway.class.getName(), type.getName()));
        }
    }

    private static Config.Organization determineOrg(ExtensionContext context, Field field) {
        Msp msp = determineMspForField(field);
        String orgName = msp.org();
        if (orgName.isBlank()) {
            throw new ExtensionConfigurationException("@Msp.org must be specified");
        }
        Store store = context.getStore(NAMESPACE);
        Config config = store.getOrDefault(CONFIG_KEY, Config.class, Config.builder().build());
        return config.getEndorsingOrganizations()
                .stream()
                .filter(o -> orgName.equals(o.getName()))
                .findFirst()
                .orElseThrow(
                        () -> new ExtensionConfigurationException("Org %s does not define in @Microfab configuration".formatted(orgName)));
    }

    private static Msp determineMspForField(Field field) {
        return findAnnotation(field, Msp.class).orElseThrow(() ->
                new ExtensionConfigurationException("Field " + field + " must be annotated with @Msp"));
    }

    @SuppressWarnings("unchecked")
    private static Gateway createGateway(ExtensionContext context, Config.Organization org) {
        return toUnchecked(
                () -> {
                    Store store = context.getStore(NAMESPACE);
                    Config config = store.getOrDefault(CONFIG_KEY, Config.class, Config.builder().build());
                    Config.Tls tls = config.getTls();
                    Map<String, State.Identity> admins = store.getOrDefault(ADMINS_KEY, Map.class, Collections.emptyMap());
                    State.Identity admin = admins.get(org.getAdminId());
                    X509Certificate cert = Identities.readX509Certificate(admin.getCertificatePem());
                    Identity identity = new X509Identity(org.getLocalMspId(), cert);
                    PrivateKey key = Identities.readPrivateKey(admin.getPrivateKeyPem());
                    Signer signer = Signers.newPrivateKeySigner(key);
                    ChannelCredentials credit;
                    if (tls.isEnabled()) {
                        credit = TlsChannelCredentials.newBuilder()
                                .trustManager(new ByteArrayInputStream(tls.getCaPem().getBytes(UTF_8)))
                                .build();
                    } else {
                        credit = InsecureChannelCredentials.create();
                    }
                    ManagedChannel grpcChannel = Grpc.newChannelBuilder(config.getApiUrl(org), credit)
                            .build();
                    Gateway.Builder builder = Gateway.newInstance()
                            .identity(identity)
                            .signer(signer)
                            .hash(Hash.SHA256)
                            .connection(grpcChannel);
                    Gateway gateway = builder.connect();
                    store.put(GATEWAY_KEY, gateway);
                    return gateway;
                });
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (context.getTestClass().isEmpty()) {
            throw new ExtensionConfigurationException("MicrofabExtension is only supported for classes.");
        }
        buildContainer(context);
        buildChaincode(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getRequiredTestInstances().getAllInstances()
                .forEach(instance -> injectInstanceFields(context, instance));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // we do not need any implementation, because all closable resources
        // will be stored in junit store and junit them on behalf of us.
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // we do not need any implementation, because all closable resources
        // will be stored in junit store and junit them on behalf of us.
    }
}
