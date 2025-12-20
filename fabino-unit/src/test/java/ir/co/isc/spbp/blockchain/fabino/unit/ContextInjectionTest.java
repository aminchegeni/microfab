package ir.co.isc.spbp.blockchain.fabino.unit;

import org.hyperledger.fabric.contract.Context;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextInjectionTest {

    @Stub
    private static Context classContext;

    private static Context beforeAllContext;

    private final Context constructorContext;

    @Stub
    private Context instanceContext;

    private Context beforeEachContext;

    ContextInjectionTest(@Stub Context paramContext) {
        this.constructorContext = paramContext;
    }

    @BeforeAll
    static void beforeAll(@Stub Context paramContext) {
        beforeAllContext = paramContext;
    }

    @BeforeEach
    void beforeEach(@Stub Context paramContext) {
        beforeEachContext = paramContext;
    }

    @RepeatedTest(100)
    void should_returnDifferentInstance_when_injectionPointIsDifferent(@Stub Context paramContext) {
        Set<String> uniques = Set.of(
                classContext.getStub().getTxId(),
                beforeAllContext.getStub().getTxId(),
                constructorContext.getStub().getTxId(),
                instanceContext.getStub().getTxId(),
                beforeEachContext.getStub().getTxId(),
                paramContext.getStub().getTxId()
        );
        assertEquals(6, uniques.size());
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestInstancePerClassTest {

        private final Context constructorContext;

        private Context beforeAllContext;

        @Stub
        private Context instanceContext;

        public TestInstancePerClassTest(@Stub Context paramContext) {
            this.constructorContext = paramContext;
        }

        @BeforeAll
        void beforeAll(@Stub Context paramContext) {
            beforeAllContext = paramContext;
        }

        @RepeatedTest(100)
        void should_returnDifferentInstance_when_injectionPointIsDifferent(@Stub Context paramContext) {
            Set<String> uniques = Set.of(
                    ContextInjectionTest.classContext.getStub().getTxId(),
                    beforeAllContext.getStub().getTxId(),
                    ContextInjectionTest.beforeAllContext.getStub().getTxId(),
                    constructorContext.getStub().getTxId(),
                    ContextInjectionTest.this.constructorContext.getStub().getTxId(),
                    instanceContext.getStub().getTxId(),
                    ContextInjectionTest.this.instanceContext.getStub().getTxId(),
                    ContextInjectionTest.this.beforeEachContext.getStub().getTxId(),
                    paramContext.getStub().getTxId()
            );
            assertEquals(9, uniques.size());
        }
    }
}
