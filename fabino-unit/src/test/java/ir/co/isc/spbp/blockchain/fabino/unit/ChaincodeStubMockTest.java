package ir.co.isc.spbp.blockchain.fabino.unit;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.Couchdb;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.InMemBlockchain;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.LedgerFacade;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.Leveldb;
import org.hyperledger.fabric.protos.peer.ChaincodeEvent;
import org.hyperledger.fabric.protos.peer.ChaincodeMessage;
import org.hyperledger.fabric.protos.peer.SignedProposal;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.QueryResultsIteratorWithMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@Ledger(
        worldState = Couchdb.class,
        seeds = {
                "key1,{\"value\":\"val1\"}",
                "key2,{\"value\":\"val2\"}",
                "key3,{\"value\":\"val3\"}",
        })
class ChaincodeStubMockTest {

    @Stub(
            mspId = "TestMSP",
            cert = """
                    -----BEGIN CERTIFICATE-----
                    MIICGjCCAcCgAwIBAgIQLuV/QbuS4AurVd2cNK2F/TAKBggqhkjOPQQDAjASMRAw
                    DgYDVQQDEwdUZXN0IENBMB4XDTI1MTIxNzA5MjczOVoXDTM1MTIxNTA5MjczOVow
                    JTEOMAwGA1UECxMFYWRtaW4xEzARBgNVBAMTClRlc3QgQWRtaW4wWTATBgcqhkjO
                    PQIBBggqhkjOPQMBBwNCAARUt8AfR2iJgCxjUZvEM6LbLKLcFdIU3zmCRUJMZISs
                    4wiTMl1UnzmSeXCfHj1xzbUZlNEdXU0W2bH1hxlrnqFko4HkMIHhMA4GA1UdDwEB
                    /wQEAwIFoDAdBgNVHSUEFjAUBggrBgEFBQcDAgYIKwYBBQUHAwEwDAYDVR0TAQH/
                    BAIwADApBgNVHQ4EIgQgwJNAkFrUsO8O1XYAe3vEEnUnZnwfY/rUZNt1Hr6QYeow
                    KwYDVR0jBCQwIoAg9kPb0+hDO6oYAtB4Qml6JPhpJTLXmFJHqYJMGJW5M+UwSgYD
                    VR0RBEMwQYISKi4xMjctMC0wLTEubmlwLmlvggkxMjcuMC4wLjGCCWxvY2FsaG9z
                    dIIHMC4wLjAuMIIMKi5sb2NhbGhvLnN0MAoGCCqGSM49BAMCA0gAMEUCIQCaL98I
                    KbbJw/KCg2bWzhyjTknluzjyM5k8SYWKX7E8jAIgPWd4KffJG+eSWweqc+7FFhHb
                    bCYj9jdQfHDlx/jonEY=
                    -----END CERTIFICATE-----
                    """,
            key = """
                    -----BEGIN PRIVATE KEY-----
                    MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgIOL0Ldedsaz1I7X0
                    YRCpT/kEmuKLRlVM0EXb4kE2ECihRANCAARUt8AfR2iJgCxjUZvEM6LbLKLcFdIU
                    3zmCRUJMZISs4wiTMl1UnzmSeXCfHj1xzbUZlNEdXU0W2bH1hxlrnqFk
                    -----END PRIVATE KEY-----
                    """,
            chaincode = @Stub.Chaincode(
                    name = "test-cc",
                    version = "1.0.0"
            ),
            channel = "test-channel",
            function = "test-function",
            args = {
                    "arg1",
                    "{\"arg\":\"arg2\"}",
                    "4"
            },
            transients = {
                    "tr1,tv1",
                    "tr2,{\"tr\":\"tv2\"}",
                    "tr3,3"
            },
            timestamp = 555_555_555_555_555L,
            nonce = {
                    1, 2, 3, 4, 5, 6,
                    2, 3, 4, 5, 6, 7,
                    3, 4, 5, 6, 7, 8,
                    4, 5, 6, 7, 8, 9
            }
    )
    private ChaincodeStubMock stub;

    @Test
    void should_throwException_when_inputChaincodeMessageIsNotValid() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new ChaincodeStubMock(
                        ChaincodeMessage.newBuilder()
                                .setPayload(ByteString.copyFromUtf8("dummy"))
                                .build(),
                        LedgerFacade.of(new Leveldb(), new InMemBlockchain())
                ));
        assertEquals("Input message is not valid", thrown.getMessage());
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertInstanceOf(InvalidProtocolBufferException.class, cause);
    }

    @Test
    void should_throwException_when_inputChaincodeMessageIsNull() {
        assertThrows(NullPointerException.class,
                () -> new ChaincodeStubMock(
                        null,
                        LedgerFacade.of(new Leveldb(), new InMemBlockchain())));
    }

    @Test
    void should_throwException_when_inputLedgerIsNull() {
        assertThrows(NullPointerException.class,
                () -> new ChaincodeStubMock(
                        ChaincodeMessage.newBuilder().build(),
                        null));
    }

    @Test
    void should_returnConfiguredChaincodeStubMock_when_inputMessageIsEmpty() {
        ChaincodeStubMock stub = new ChaincodeStubMock(
                ChaincodeMessage.newBuilder().build(),
                LedgerFacade.of(new Leveldb(), new InMemBlockchain()));

        assertNull(stub.getFunction());

        stub.setEvent("event1", new byte[]{65, 66});
        assertEquals(
                ChaincodeEvent.newBuilder()
                        .setEventName("event1")
                        .setPayload(ByteString.copyFrom(new byte[]{65, 66}))
                        .build(),
                stub.getEvent());

        stub.setEvent("event2", null);
        assertEquals(
                ChaincodeEvent.newBuilder()
                        .setEventName("event2")
                        .build(),
                stub.getEvent());

        assertEquals(SignedProposal.newBuilder().build(), stub.getSignedProposal());
    }

    @Test
    void should_returnUpdatedState_when_inputStateKeyExists(@Stub ChaincodeStubMock stub) {
        byte[] old = stub.getState("key2");
        assertNotNull(old);
        stub.putState("key2", "val2".getBytes(UTF_8));
        try {
            TimeUnit.SECONDS.sleep(3L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        byte[] recent = stub.getState("key2");
        assertNotEquals(old.length, recent.length);
        assertEquals(2,
                StreamSupport.stream(stub.getHistoryForKey("key2").spliterator(), false).count());
    }

    @Test
    void should_returnNull_when_inputStateKeyExistsAndThenDeleted(@Stub ChaincodeStubMock stub) {
        assertNotNull(stub.getState("key1"));
        stub.delState("key1");
        try {
            TimeUnit.SECONDS.sleep(3L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertNull(stub.getState("key1"));
        assertEquals(2,
                StreamSupport.stream(stub.getHistoryForKey("key1").spliterator(), false).count());
    }

    @Test
    void should_returnNull_when_inputStateKeyDoseNotExistAndThenDeleted(@Stub ChaincodeStubMock stub) {
        assertNull(stub.getState("key4"));
        stub.delState("key4");
        assertNull(stub.getState("key4"));
        assertEquals(0,
                StreamSupport.stream(stub.getHistoryForKey("key4").spliterator(), false).count());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_throwException_when_inputEventNameIsNotValid(String name) {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> stub.setEvent(name, new byte[0]));
        assertEquals("Event name can not be nil string", thrown.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_throwException_when_inputKeyIsNotValid(String key) {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> stub.getState(key));
        assertEquals("Key can not be null or blank string", thrown.getMessage());
    }

    @Test
    void should_returnConfiguredChaincodeStubMock_when_stubAndLedgerAnnotationPropertiesAreNotDefault() {

        List<String> args = List.of("test-function", "arg1", "{\"arg\":\"arg2\"}", "4");

        assertTrue(
                IntStream.range(0, 4)
                        .allMatch(i -> Arrays.equals(args.get(i).getBytes(UTF_8), stub.getArgs().get(i))));

        assertEquals(args, stub.getStringArgs());

        assertEquals("test-function", stub.getFunction());

        assertEquals(List.of("arg1", "{\"arg\":\"arg2\"}", "4"), stub.getParameters());

        String txId = MessageFactory.txId(
                new byte[]{
                        1, 2, 3, 4, 5, 6,
                        2, 3, 4, 5, 6, 7,
                        3, 4, 5, 6, 7, 8,
                        4, 5, 6, 7, 8, 9
                }, stub.getCreator());

        assertEquals(txId, stub.getTxId());

        assertEquals("test-channel", stub.getChannelId());

        assertSame(Chaincode.Response.Status.INTERNAL_SERVER_ERROR,
                stub.invokeChaincode("test-cc", List.of(), "test-channel").getStatus());

        assertSame(Chaincode.Response.Status.INTERNAL_SERVER_ERROR,
                stub.invokeChaincode("test-cc", List.of()).getStatus());

        assertSame(Chaincode.Response.Status.INTERNAL_SERVER_ERROR,
                stub.invokeChaincodeWithStringArgs("test-cc").getStatus());

        assertSame(Chaincode.Response.Status.INTERNAL_SERVER_ERROR,
                stub.invokeChaincodeWithStringArgs("test-cc", List.of()).getStatus());

        assertSame(Chaincode.Response.Status.INTERNAL_SERVER_ERROR,
                stub.invokeChaincodeWithStringArgs("test-cc", List.of(), "test-channel").getStatus());

        assertArrayEquals("{\"value\":\"val1\"}".getBytes(UTF_8), stub.getState("key1"));

        assertEquals("{\"value\":\"val1\"}", stub.getStringState("key1"));

        assertArrayEquals(new byte[0], stub.getStateValidationParameter("key1"));

        assertDoesNotThrow(() -> stub.setStateValidationParameter("key1", new byte[0]));

        var stateRange = stub.getStateByRange("key1", "key2");
        assertFalse(stateRange.iterator().hasNext());
        assertDoesNotThrow(stateRange::close);

        var stateRangeWithPagination = stub.getStateByRangeWithPagination("key1", "key2", 2, "");
        assertFalse(stateRangeWithPagination.iterator().hasNext());
        assertInstanceOf(QueryResultsIteratorWithMetadata.class, stateRangeWithPagination);
        assertDoesNotThrow(stateRangeWithPagination::close);
        assertNotNull(stateRangeWithPagination.getMetadata());

        assertFalse(stub.getStateByPartialCompositeKey("key1").iterator().hasNext());

        assertFalse(stub.getStateByPartialCompositeKey("key1", "key2").iterator().hasNext());

        assertFalse(stub.getStateByPartialCompositeKey(new CompositeKey("key1")).iterator().hasNext());

        assertFalse(
                stub.getStateByPartialCompositeKeyWithPagination(new CompositeKey("key1"), 1, "")
                        .iterator().hasNext());

        assertEquals(new CompositeKey("key1", "key2").toString(),
                stub.createCompositeKey("key1", "key2").toString());

        assertEquals(new CompositeKey("key2", "key1").toString(),
                stub.splitCompositeKey(new CompositeKey("key2", "key1").toString()).toString());

        assertFalse(stub.getQueryResult("key1").iterator().hasNext());

        assertFalse(stub.getQueryResultWithPagination("key1", 1, "").iterator().hasNext());

        assertTrue(stub.getHistoryForKey("key1").iterator().hasNext());

        assertArrayEquals(new byte[0], stub.getPrivateData("", "key1"));

        assertEquals("", stub.getPrivateDataUTF8("", "key1"));

        assertArrayEquals(new byte[0], stub.getPrivateDataHash("", "key1"));

        assertArrayEquals(new byte[0], stub.getPrivateDataValidationParameter("", "key1"));

        assertDoesNotThrow(() -> stub.putPrivateData("", "key1", new byte[0]));

        assertDoesNotThrow(() -> stub.setPrivateDataValidationParameter("", "key1", new byte[0]));

        assertDoesNotThrow(() -> stub.delPrivateData("", "key1"));

        assertDoesNotThrow(() -> stub.purgePrivateData("", "key1"));

        assertFalse(stub.getPrivateDataByRange("", "key1", "key2").iterator().hasNext());

        assertFalse(stub.getPrivateDataQueryResult("", "key1").iterator().hasNext());

        assertFalse(stub.getPrivateDataByPartialCompositeKey("", "key1").iterator().hasNext());

        assertFalse(
                stub.getPrivateDataByPartialCompositeKey("", "key1", "key2")
                        .iterator().hasNext());

        assertFalse(
                stub.getPrivateDataByPartialCompositeKey("", new CompositeKey("key1"))
                        .iterator().hasNext());

        assertNull(stub.getEvent());

        assertEquals(Instant.ofEpochMilli(555_555_555_555_555L), stub.getTxTimestamp());

        Map<String, byte[]> transients = Map.of(
                "tr1", "tv1".getBytes(UTF_8),
                "tr2", "{\"tr\":\"tv2\"}".getBytes(UTF_8),
                "tr3", "3".getBytes(UTF_8)
        );
        assertEquals(transients.keySet(), stub.getTransient().keySet());
        transients.forEach((k, v) -> assertArrayEquals(v, stub.getTransient().get(k)));

        assertArrayEquals(new byte[0], stub.getBinding());

        assertEquals("MockMSP", stub.getMspId());
    }
}
