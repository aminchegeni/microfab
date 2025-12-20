package ir.co.isc.spbp.blockchain.fabino.unit;

import com.google.protobuf.Timestamp;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.hyperledger.fabric.client.Hash;
import org.hyperledger.fabric.shim.ChaincodeStub;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ir.co.isc.spbp.blockchain.fabino.unit.Stub.*;
import static ir.co.isc.spbp.blockchain.fabino.unit.Stub.Chaincode.DEFAULT_CC_NAME;
import static ir.co.isc.spbp.blockchain.fabino.unit.Stub.Chaincode.DEFAULT_CC_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;

/**
 * Immutable context describing a mocked chaincode invocation.
 * <p>
 * This class aggregates all inputs required to construct a Fabric
 * {@link ChaincodeStub} for testing purposes, including identity
 * information, chaincode metadata, invocation arguments, transient
 * data, timestamp, and nonce.
 * <p>
 * Default values are provided to minimize boilerplate while still
 * allowing deterministic or customized behavior when required.
 */
@Value
@Builder
public class StubInvocationContext {

    /**
     * Length of the Fabric transaction nonce in bytes.
     */
    private static final int NONCE_LENGTH = 24;

    /**
     * Secure random generator used for nonce creation.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * MSP identifier of the transaction creator.
     */
    @NonNull
    @lombok.Builder.Default
    String mspId = DEFAULT_MSP_ID;

    /**
     * PEM-encoded X.509 certificate of the transaction creator.
     */
    @NonNull
    @lombok.Builder.Default
    String cert = DEFAULT_CERT;

    /**
     * Private key corresponding to the creator certificate.
     */
    @NonNull
    @lombok.Builder.Default
    String key = DEFAULT_KEY;

    /**
     * Chaincode name and version targeted by the invocation.
     */
    @NonNull
    @lombok.Builder.Default
    ChaincodeContext chaincode = ChaincodeContext.builder().build();

    /**
     * Channel name on which the transaction is executed.
     */
    @NonNull
    @lombok.Builder.Default
    String channel = DEFAULT_CHANNEL;

    // TODO: handle defensive copy
    /**
     * Chaincode invocation arguments.
     * <p>
     * The first element represents the function name.
     */
    @NonNull
    @lombok.Builder.Default
    List<String> args = List.of(DEFAULT_FUNCTION);

    // TODO: handle defensive copy
    /**
     * Transient data supplied with the transaction.
     * <p>
     * These values are not persisted on the ledger.
     */
    @NonNull
    @lombok.Builder.Default
    Map<String, String> transients = Collections.emptyMap();

    /**
     * Transaction timestamp exposed via
     * {@link org.hyperledger.fabric.shim.ChaincodeStub#getTxTimestamp()}.
     * <p>
     * When the default value is used, the current system time is applied.
     */
    @NonNull
    @lombok.Builder.Default
    Timestamp timestamp = timestamp(DEFAULT_TIMESTAMP_MILLIS);

    // TODO: handle defensive copy
    /**
     * Nonce used when constructing the transaction signature header.
     * <p>
     * When the default value is used, a secure random nonce is generated.
     */
    @lombok.Builder.Default
    byte @NonNull [] nonce = nonce(new byte[0]);

    /**
     * Computes a deterministic hash representing this invocation context.
     * <p>
     * The hash is derived from the textual representation of the context and
     * produced using SHA-256, yielding a stable and collision-resistant key.
     * <p>
     * Intended for use as a unique identifier when storing data in a JUnit
     * {@link org.junit.jupiter.api.extension.ExtensionContext.Store}, ensuring
     * safe isolation between concurrent or repeated test executions.
     *
     * @return hexadecimal SHA-256 hash of this invocation context
     */
    public String hash() {
        return HexFormat.of().formatHex(Hash.SHA256.apply(toString().getBytes(UTF_8)));
    }

    /**
     * Creates an invocation context from a {@link Stub} annotation.
     *
     * @param stub stub annotation defining invocation parameters
     * @return resolved invocation context
     */
    public static StubInvocationContext of(Stub stub) {
        return StubInvocationContext.builder()
                .mspId(stub.mspId())
                .cert(stub.cert())
                .key(stub.key())
                .chaincode(ChaincodeContext.builder()
                        .name(stub.chaincode().name())
                        .version(stub.chaincode().version())
                        .build())
                .channel(stub.channel())
                .args(Stream.concat(Stream.of(stub.function()), Stream.of(stub.args())).toList())
                .transients(transients(stub.transients()))
                .timestamp(timestamp(stub.timestamp()))
                .nonce(nonce(stub.nonce()))
                .build();
    }

    /**
     * Determines whether the provided nonce represents the default value.
     */
    private static boolean isDefaultNonce(byte[] nonce) {
        return IntStream.range(0, nonce.length).allMatch(i -> nonce[i] == 0);
    }

    /**
     * Determines whether the provided timestamp represents the default value.
     */
    private static boolean isDefaultTimestamp(long millis) {
        return millis == DEFAULT_TIMESTAMP_MILLIS;
    }

    /**
     * Converts milliseconds since epoch into a Fabric {@link Timestamp}.
     * <p>
     * Uses the current system time when the default value is detected.
     */
    private static Timestamp timestamp(long millis) {
        if (isDefaultTimestamp(millis)) {
            millis = System.currentTimeMillis();
        }
        return Timestamp.newBuilder()
                .setSeconds(millis / 1_000)
                .setNanos((int) ((millis % 1_000) * 1_000_000))
                .build();
    }

    /**
     * Resolves the effective nonce, generating a random value when the
     * default nonce is detected.
     */
    private static byte[] nonce(byte[] provided) {
        if (isDefaultNonce(provided)) {
            return randomNonce();
        }
        byte[] nonce = new byte[NONCE_LENGTH];
        System.arraycopy(provided, 0, nonce, 0, Math.min(provided.length, NONCE_LENGTH));
        return nonce;
    }

    /**
     * Generates a cryptographically secure random nonce.
     */
    private static byte[] randomNonce() {
        byte[] values = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(values);
        return values;
    }

    /**
     * Parses transient data entries from {@code key,value} pairs.
     */
    private static Map<String, String> transients(String[] csv) {
        return Stream.of(csv)
                .distinct()
                .filter(not(String::isBlank))
                .map(line -> line.split(",", 2))
                .filter(kv -> kv.length == 2)
                .collect(Collectors.toUnmodifiableMap(kv -> kv[0], kv -> kv[1]));
    }

    /**
     * Chaincode identification metadata used for the invocation.
     */
    @Value
    @lombok.Builder
    public static class ChaincodeContext {

        /**
         * Chaincode name.
         */
        @NonNull
        @lombok.Builder.Default
        String name = DEFAULT_CC_NAME;

        /**
         * Chaincode version.
         */
        @NonNull
        @lombok.Builder.Default
        String version = DEFAULT_CC_VERSION;
    }
}
