package ir.co.isc.spbp.blockchain.fabino.unit;

import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle;

class ChaincodeStubInjectionTest {

    private static final Set<String> UNIQUES = Collections.synchronizedSet(new HashSet<>());

    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(0);

    @Stub
    private static ChaincodeStub classStub;

    private static ChaincodeStub beforeAllStub;

    private final ChaincodeStub constructorStub;

    @Stub
    private ChaincodeStub instanceStub;

    private ChaincodeStub beforeEachStub;

    ChaincodeStubInjectionTest(@Stub ChaincodeStub paramStub) {
        this.constructorStub = paramStub;
    }

    @BeforeAll
    static void beforeAll(@Stub ChaincodeStub paramStub) {
        beforeAllStub = paramStub;
    }

    @AfterAll
    static void afterAll() {
        UNIQUES.clear();
        INSTANCE_COUNTER.set(0);
    }

    @BeforeEach
    void beforeEach(@Stub ChaincodeStub paramStub) {
        beforeEachStub = paramStub;
    }

    @RepeatedTest(100)
    void should_returnSameInstancePerIteration_when_injectionPointIsClassField(RepetitionInfo info) {
        String txId = classStub.getTxId();
        UNIQUES.add(txId);
        if (info.getCurrentRepetition() <= 1) {
            INSTANCE_COUNTER.getAndIncrement();
        }
        assertNotEquals(txId, beforeAllStub.getTxId());
        assertTrue(UNIQUES.contains(txId));
        assertEquals(INSTANCE_COUNTER.get(), UNIQUES.size());
    }

    @RepeatedTest(100)
    void should_returnSameInstancePerIteration_when_injectionPointIsBeforeAllMethodParam(RepetitionInfo info) {
        String txId = beforeAllStub.getTxId();
        UNIQUES.add(txId);
        if (info.getCurrentRepetition() <= 1) {
            INSTANCE_COUNTER.getAndIncrement();
        }
        assertNotEquals(txId, classStub.getTxId());
        assertTrue(UNIQUES.contains(txId));
        assertEquals(INSTANCE_COUNTER.get(), UNIQUES.size());
    }

    @RepeatedTest(100)
    void should_returnNewInstancePerIteration_when_injectionPointIsConstructorParam() {
        String txId = constructorStub.getTxId();
        UNIQUES.add(txId);
        assertNotEquals(txId, classStub.getTxId());
        assertNotEquals(txId, beforeAllStub.getTxId());
        assertNotEquals(txId, instanceStub.getTxId());
        assertNotEquals(txId, beforeEachStub.getTxId());
        assertTrue(UNIQUES.contains(txId));
        assertEquals(INSTANCE_COUNTER.incrementAndGet(), UNIQUES.size());
    }

    @RepeatedTest(100)
    void should_returnNewInstancePerIteration_when_injectionPointIsInstanceField() {
        String txId = instanceStub.getTxId();
        UNIQUES.add(txId);
        assertNotEquals(txId, classStub.getTxId());
        assertNotEquals(txId, beforeAllStub.getTxId());
        assertNotEquals(txId, constructorStub.getTxId());
        assertNotEquals(txId, beforeEachStub.getTxId());
        assertTrue(UNIQUES.contains(txId));
        assertEquals(INSTANCE_COUNTER.incrementAndGet(), UNIQUES.size());
    }

    @RepeatedTest(100)
    void should_returnNewInstancePerIteration_when_injectionPointIsBeforeEachMethodParam() {
        String txId = beforeEachStub.getTxId();
        UNIQUES.add(txId);
        assertNotEquals(txId, classStub.getTxId());
        assertNotEquals(txId, beforeAllStub.getTxId());
        assertNotEquals(txId, instanceStub.getTxId());
        assertNotEquals(txId, constructorStub.getTxId());
        assertTrue(UNIQUES.contains(txId));
        assertEquals(INSTANCE_COUNTER.incrementAndGet(), UNIQUES.size());
    }

    @RepeatedTest(100)
    void should_returnNewInstancePerIteration_when_injectionPointIsTestMethodParam(@Stub ChaincodeStub paramStub) {
        String txId = paramStub.getTxId();
        UNIQUES.add(txId);
        assertNotEquals(txId, classStub.getTxId());
        assertNotEquals(txId, beforeAllStub.getTxId());
        assertNotEquals(txId, instanceStub.getTxId());
        assertNotEquals(txId, constructorStub.getTxId());
        assertNotEquals(txId, beforeEachStub.getTxId());
        assertTrue(UNIQUES.contains(txId));
        assertEquals(INSTANCE_COUNTER.incrementAndGet(), UNIQUES.size());
    }

    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    class TestInstancePerClassTest {

        private final ChaincodeStub constructorStub;

        private ChaincodeStub beforeAllStub;

        @Stub
        private ChaincodeStub instanceStub;

        public TestInstancePerClassTest(@Stub ChaincodeStub paramStub) {
            this.constructorStub = paramStub;
        }

        @BeforeAll
        void beforeAll(@Stub ChaincodeStub paramStub) {
            beforeAllStub = paramStub;
        }

        @RepeatedTest(100)
        void should_returnSameInstancePerIteration_when_injectionPointIsBeforeAllMethodParam(RepetitionInfo info) {
            String txId = beforeAllStub.getTxId();
            UNIQUES.add(txId);
            if (info.getCurrentRepetition() <= 1) {
                INSTANCE_COUNTER.getAndIncrement();
            }
            assertNotEquals(txId, ChaincodeStubInjectionTest.beforeAllStub.getTxId());
            assertTrue(UNIQUES.contains(txId));
            assertEquals(INSTANCE_COUNTER.get(), UNIQUES.size());
        }

        @RepeatedTest(100)
        void should_returnSameInstancePerIteration_when_injectionPointIsConstructorParam(RepetitionInfo info) {
            String txId = constructorStub.getTxId();
            UNIQUES.add(txId);
            if (info.getCurrentRepetition() <= 1) {
                INSTANCE_COUNTER.getAndIncrement();
            }
            assertNotEquals(txId, ChaincodeStubInjectionTest.this.constructorStub.getTxId());
            assertTrue(UNIQUES.contains(txId));
            assertEquals(INSTANCE_COUNTER.get(), UNIQUES.size());
        }

        @RepeatedTest(100)
        void should_returnNewInstancePerIteration_when_injectionPointIsInstanceField() {
            String txId = instanceStub.getTxId();
            UNIQUES.add(txId);
            assertNotEquals(txId, ChaincodeStubInjectionTest.this.instanceStub.getTxId());
            assertTrue(UNIQUES.contains(txId));
            assertEquals(INSTANCE_COUNTER.incrementAndGet(), UNIQUES.size());
        }
    }
}
