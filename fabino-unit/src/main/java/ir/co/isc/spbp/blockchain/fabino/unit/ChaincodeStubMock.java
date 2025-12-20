package ir.co.isc.spbp.blockchain.fabino.unit;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.Block;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.LedgerFacade;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.Transaction;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.WorldState;
import org.hyperledger.fabric.protos.common.ChannelHeader;
import org.hyperledger.fabric.protos.common.Header;
import org.hyperledger.fabric.protos.common.SignatureHeader;
import org.hyperledger.fabric.protos.peer.*;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ResponseUtils;
import org.hyperledger.fabric.shim.ledger.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.*;

/**
 * In-memory {@link ChaincodeStub} implementation for unit testing.
 * <p>
 * This class adapts a {@link ChaincodeMessage} and a test {@link LedgerFacade} into
 * a usable {@code ChaincodeStub}, allowing chaincode logic to be executed
 * without a running peer or ordering service.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Extracts invocation arguments, creator identity, timestamp, and
 *       transient data from the incoming proposal.</li>
 *   <li>Delegates state reads and writes to the provided {@link LedgerFacade}.</li>
 *   <li>Records state mutations as {@link ir.co.isc.spbp.blockchain.fabino.unit.ledger.Transaction}s grouped into
 *       {@link Block}s on the mock blockchain.</li>
 *   <li>Exposes a minimal subset of query and history APIs required by
 *       typical chaincode logic.</li>
 * </ul>
 *
 * <h2>Behavioral Notes</h2>
 * <ul>
 *   <li>Each state mutation results in a new block containing a single
 *       transaction.</li>
 *   <li>Transaction history is derived by replaying committed blocks.</li>
 *   <li>Rich queries are conditionally supported based on the
 *       {@link WorldState.Type}.</li>
 *   <li>Unimplemented Fabric features fail fast or return empty results.</li>
 * </ul>
 *
 * <p>
 * This implementation is intentionally incomplete and opinionated. It is
 * designed to support deterministic testing of chaincode behavior rather
 * than faithfully reproducing peer internals.
 */
public class ChaincodeStubMock implements ChaincodeStub {

    private static final String CORE_PEER_LOCAL_MSP_ID = "CORE_PEER_LOCALMSPID";

    private final ChaincodeMessage message;

    private final LedgerFacade ledger;

    private final List<ByteString> args;

    private final Timestamp timestamp;

    private final ByteString creator;

    private final Map<String, ByteString> transients;

    private ChaincodeEvent event;

    public ChaincodeStubMock(ChaincodeMessage message, LedgerFacade ledger) {
        this.message = requireNonNull(message);
        this.ledger = requireNonNull(ledger);
        try {
            final ChaincodeInput input = ChaincodeInput.parseFrom(message.getPayload());
            final SignedProposal signedProposal = message.getProposal();
            final Proposal proposal = Proposal.parseFrom(signedProposal.getProposalBytes());
            final Header header = Header.parseFrom(proposal.getHeader());
            final ChannelHeader channelHeader = ChannelHeader.parseFrom(header.getChannelHeader());
            final SignatureHeader signatureHeader = SignatureHeader.parseFrom(header.getSignatureHeader());
            final var chaincodeProposalPayload = ChaincodeProposalPayload.parseFrom(proposal.getPayload());
            this.args = Collections.unmodifiableList(input.getArgsList());
            this.timestamp = channelHeader.getTimestamp();
            this.creator = signatureHeader.getCreator();
            this.transients = chaincodeProposalPayload.getTransientMapMap();
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Input message is not valid", e);
        }
    }

    private static <T> NonClosableQueryResultsIterator<T> emptyQueryResultsIterator() {
        return Collections::emptyIterator;
    }

    private static <T> NonClosableQueryResultsIteratorWithMetadata<T> emptyQueryResultsIteratorWithMetadata() {
        return Collections::emptyIterator;
    }

    @Override
    public List<byte[]> getArgs() {
        return args.stream()
                .map(ByteString::toByteArray)
                .toList();
    }

    @Override
    public List<String> getStringArgs() {
        return args.stream()
                .map(ByteString::toStringUtf8)
                .toList();
    }

    @Override
    public String getFunction() {
        return getStringArgs().isEmpty() ? null : getStringArgs().getFirst();
    }

    @Override
    public List<String> getParameters() {
        return getStringArgs().stream().skip(1).toList();
    }

    @Override
    public String getTxId() {
        return message.getTxid();
    }

    @Override
    public String getChannelId() {
        return message.getChannelId();
    }

    @Override
    public Chaincode.Response invokeChaincode(String chaincodeName, List<byte[]> args, String channel) {
        return ResponseUtils.newErrorResponse();
    }

    @Override
    public byte[] getState(String key) {
        validateKey(key);
        return Optional.of(ledger)
                .map(l -> l.getState(key))
                .map(v -> v.getBytes(UTF_8))
                .orElse(null);
    }

    @Override
    public byte[] getStateValidationParameter(String key) {
        return new byte[0];
    }

    @Override
    public synchronized void putState(String key, byte[] value) {
        validateKey(key);
        ledger.submit(new Transaction(key, getTxId(), value, getTxTimestamp(), false));
    }

    @Override
    public void setStateValidationParameter(String key, byte[] value) {
        // not-implemented
    }

    @Override
    public synchronized void delState(String key) {
        validateKey(key);
        String value = ledger.getState(key);
        if (nonNull(value)) {
            ledger.submit(new Transaction(key, getTxId(), value.getBytes(UTF_8), getTxTimestamp(), true));
        }
    }

    @Override
    public QueryResultsIterator<KeyValue> getStateByRange(String startKey, String endKey) {
        return emptyQueryResultsIterator();
    }

    @Override
    public QueryResultsIteratorWithMetadata<KeyValue> getStateByRangeWithPagination(String startKey, String endKey, int pageSize, String bookmark) {
        return emptyQueryResultsIteratorWithMetadata();
    }

    @Override
    public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String compositeKey) {
        return emptyQueryResultsIterator();
    }

    @Override
    public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String objectType, String... attributes) {
        return emptyQueryResultsIterator();
    }

    @Override
    public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(CompositeKey compositeKey) {
        return emptyQueryResultsIterator();
    }

    @Override
    public QueryResultsIteratorWithMetadata<KeyValue> getStateByPartialCompositeKeyWithPagination(CompositeKey compositeKey, int pageSize, String bookmark) {
        return emptyQueryResultsIteratorWithMetadata();
    }

    @Override
    public CompositeKey createCompositeKey(String objectType, String... attributes) {
        return new CompositeKey(objectType, attributes);
    }

    @Override
    public CompositeKey splitCompositeKey(String compositeKey) {
        return CompositeKey.parseCompositeKey(compositeKey);
    }

    @Override
    public QueryResultsIterator<KeyValue> getQueryResult(String query) {
        return emptyQueryResultsIterator();
    }

    @Override
    public QueryResultsIteratorWithMetadata<KeyValue> getQueryResultWithPagination(String query, int pageSize, String bookmark) {
        return emptyQueryResultsIteratorWithMetadata();
    }

    @Override
    public synchronized NonClosableQueryResultsIterator<KeyModification> getHistoryForKey(String key) {
        validateKey(key);
        return () -> ledger.getBlocks()
                .stream()
                .map(Block::getTransactions)
                .flatMap(List<Transaction>::stream)
                .filter(tx -> key.equals(tx.getKey()))
                .map(KeyModification.class::cast)
                .iterator();
    }

    @Override
    public byte[] getPrivateData(String collection, String key) {
        return new byte[0];
    }

    @Override
    public byte[] getPrivateDataHash(String collection, String key) {
        return new byte[0];
    }

    @Override
    public byte[] getPrivateDataValidationParameter(String collection, String key) {
        return new byte[0];
    }

    @Override
    public void putPrivateData(String collection, String key, byte[] value) {
        // not-implemented
    }

    @Override
    public void setPrivateDataValidationParameter(String collection, String key, byte[] value) {
        // not-implemented
    }

    @Override
    public void delPrivateData(String collection, String key) {
        // not-implemented
    }

    @Override
    public void purgePrivateData(String collection, String key) {
        // not-implemented
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataByRange(String collection, String startKey, String endKey) {
        return emptyQueryResultsIterator();
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String compositeKey) {
        return emptyQueryResultsIterator();
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, CompositeKey compositeKey) {
        return emptyQueryResultsIterator();
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String objectType, String... attributes) {
        return emptyQueryResultsIterator();
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataQueryResult(String collection, String query) {
        return emptyQueryResultsIterator();
    }

    @Override
    public void setEvent(String name, byte[] payload) {
        if (isNull(name) || name.isBlank()) {
            throw new IllegalArgumentException("Event name can not be nil string");
        }
        ChaincodeEvent.Builder builder = ChaincodeEvent.newBuilder()
                .setEventName(name);
        if (nonNull(payload)) {
            builder.setPayload(ByteString.copyFrom(payload));
        }
        this.event = builder.build();
    }

    @Override
    public ChaincodeEvent getEvent() {
        return this.event;
    }

    @Override
    public SignedProposal getSignedProposal() {
        return message.getProposal();
    }

    @Override
    public Instant getTxTimestamp() {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    @Override
    public byte[] getCreator() {
        if (nonNull(creator)) {
            return creator.toByteArray();
        }
        return null;
    }

    @Override
    public Map<String, byte[]> getTransient() {
        return transients.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().toByteArray()));
    }

    @Override
    public byte[] getBinding() {
        return new byte[0];
    }

    @Override
    public String getMspId() {
        if (System.getenv().containsKey(CORE_PEER_LOCAL_MSP_ID)) {
            return System.getenv(CORE_PEER_LOCAL_MSP_ID);
        }
        return "MockMSP";
    }

    private void validateKey(final String key) {
        if (isNull(key) || key.isBlank()) {
            throw new IllegalArgumentException("Key can not be null or blank string");
        }
    }

    @FunctionalInterface
    public interface NonClosableQueryResultsIterator<T> extends QueryResultsIterator<T> {

        default void close() {}
    }

    @FunctionalInterface
    public interface NonClosableQueryResultsIteratorWithMetadata<T> extends QueryResultsIteratorWithMetadata<T> {

        default void close() {}

        @Override
        default QueryResponseMetadata getMetadata() {
            return QueryResponseMetadata.getDefaultInstance();
        }
    }
}
