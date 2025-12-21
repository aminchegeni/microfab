package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

import ir.co.isc.spbp.blockchain.fabino.unit.Ledger;
import ir.co.isc.spbp.blockchain.fabino.unit.Stub;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.*;

import java.util.stream.StreamSupport;

import static ir.co.isc.spbp.blockchain.fabino.unit.Ledger.Scope;
import static ir.co.isc.spbp.blockchain.fabino.unit.Utils.waitForCommit;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

// This ledger is shared across all test methods in this class.
// Any modification will persist and be visible to subsequent tests.
@Ledger(
        scope = Scope.CLASS,
        worldState = Couchdb.class,
        seeds = {
                """
                        key1,{
                          "version": 0,
                          "value": "val1"
                        }""",
                """
                        key2,{
                          "version": 0,
                          "value": "val2"
                        }""",
                """
                        key3,{
                          "version": 0,
                          "value": "val3"
                        }"""
        }
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgerClassScopeSharingTest {

    @Stub
    private static ChaincodeStub classStub;

    private static ChaincodeStub beforeAllStub;

    private final ChaincodeStub constructorStub;

    private ChaincodeStub beforeEachStub;

    @Stub
    private ChaincodeStub instanceStub;

    public LedgerClassScopeSharingTest(@Stub ChaincodeStub paramStub) {
        this.constructorStub = paramStub;
    }

    @BeforeAll
    static void beforeAll(@Stub ChaincodeStub paramStub) {
        beforeAllStub = paramStub;
    }

    @BeforeEach
    void beforeEach(@Stub ChaincodeStub paramStub) {
        beforeEachStub = paramStub;
    }

    @Test
    @Order(1)
    void should_returnSingleHistory_when_queryPreExistedKey() {
        QueryResultsIterator<KeyModification> historiesForKey1 = beforeEachStub.getHistoryForKey("key1");
        assertEquals(1, StreamSupport.stream(historiesForKey1.spliterator(), false).count());
        assertTrue(historiesForKey1.iterator().hasNext());
        KeyModification modificationForKey1 = historiesForKey1.iterator().next();
        assertEquals("F".repeat(64), modificationForKey1.getTxId());
        assertEquals("""
                {
                  "version": 0,
                  "value": "val1"
                }""", modificationForKey1.getStringValue());
        assertFalse(modificationForKey1.isDeleted());

        QueryResultsIterator<KeyModification> historiesForKey2 = beforeEachStub.getHistoryForKey("key2");
        assertEquals(1, StreamSupport.stream(historiesForKey2.spliterator(), false).count());
        assertTrue(historiesForKey2.iterator().hasNext());
        KeyModification modificationForKey2 = historiesForKey2.iterator().next();
        assertEquals("F".repeat(64), modificationForKey2.getTxId());
        assertEquals("""
                {
                  "version": 0,
                  "value": "val2"
                }""", modificationForKey2.getStringValue());
        assertFalse(modificationForKey2.isDeleted());

        QueryResultsIterator<KeyModification> historiesForKey3 = beforeEachStub.getHistoryForKey("key3");
        assertEquals(1, StreamSupport.stream(historiesForKey3.spliterator(), false).count());
        assertTrue(historiesForKey3.iterator().hasNext());
        KeyModification modificationForKey3 = historiesForKey3.iterator().next();
        assertEquals("F".repeat(64), modificationForKey3.getTxId());
        assertEquals("""
                {
                  "version": 0,
                  "value": "val3"
                }""", modificationForKey3.getStringValue());
        assertFalse(modificationForKey3.isDeleted());
    }

    @Test
    @Order(2)
    void should_returnValue_when_keyExists() {
        byte[] value = instanceStub.getState("key1");
        assertNotEquals(0, value.length);
        assertEquals("""
                {
                  "version": 0,
                  "value": "val1"
                }""", new String(value, UTF_8));
    }

    @Test
    @Order(3)
    void should_returnChangedValue_when_keyExists() {
        classStub.putState("key2", """
                {
                  "version": 1,
                  "value": "new-val2"
                }""".getBytes(UTF_8));

        waitForCommit();

        byte[] value = classStub.getState("key2");
        assertNotEquals(0, value.length);
        assertEquals("""
                {
                  "version": 1,
                  "value": "new-val2"
                }""", new String(value, UTF_8));
    }

    @Test
    @Order(4)
    void should_returnNull_when_keyExistsAndThenDeleted() {
        byte[] value = constructorStub.getState("key3");
        assertNotEquals(0, value.length);
        assertEquals("""
                {
                  "version": 0,
                  "value": "val3"
                }""", new String(value, UTF_8));

        constructorStub.delState("key3");

        waitForCommit();

        value = constructorStub.getState("key3");
        assertNull(value);
    }

    @Test
    @Order(5)
    void should_returnCreatedValue_when_keyDoesNotExist() {
        byte[] value = beforeAllStub.getState("key4");
        assertNull(value);

        beforeAllStub.putState("key4", """
                {
                  "version": 0,
                  "value": "val4"
                }""".getBytes(UTF_8));

        waitForCommit();

        value = beforeAllStub.getState("key4");
        assertNotEquals(0, value.length);
        assertEquals("""
                {
                  "version": 0,
                  "value": "val4"
                }""", new String(value, UTF_8));
    }

    @Test
    @Order(6)
    void should_returnHistories_when_keysWereModified(@Stub ChaincodeStub paramStub) {
        QueryResultsIterator<KeyModification> historiesForKey1 = paramStub.getHistoryForKey("key1");
        assertEquals(1, StreamSupport.stream(historiesForKey1.spliterator(), false).count());
        assertTrue(historiesForKey1.iterator().hasNext());
        KeyModification modificationForKey1 = historiesForKey1.iterator().next();
        assertEquals("F".repeat(64), modificationForKey1.getTxId());
        assertEquals("""
                {
                  "version": 0,
                  "value": "val1"
                }""", modificationForKey1.getStringValue());
        assertFalse(modificationForKey1.isDeleted());

        QueryResultsIterator<KeyModification> historiesForKey2 = paramStub.getHistoryForKey("key2");
        assertEquals(2, StreamSupport.stream(historiesForKey2.spliterator(), false).count());

        QueryResultsIterator<KeyModification> historiesForKey3 = paramStub.getHistoryForKey("key3");
        assertEquals(2, StreamSupport.stream(historiesForKey3.spliterator(), false).count());

        QueryResultsIterator<KeyModification> historiesForKey4 = paramStub.getHistoryForKey("key4");
        assertEquals(1, StreamSupport.stream(historiesForKey4.spliterator(), false).count());
        assertTrue(historiesForKey4.iterator().hasNext());
        KeyModification modificationForKey4 = historiesForKey4.iterator().next();
        assertNotEquals("F".repeat(64), modificationForKey4.getTxId());
        assertEquals("""
                {
                  "version": 0,
                  "value": "val4"
                }""", modificationForKey4.getStringValue());
        assertFalse(modificationForKey4.isDeleted());
    }
}
