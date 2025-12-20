package ir.co.isc.spbp.blockchain.fabino.unit;

import org.hyperledger.fabric.protos.peer.SignedProposal;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares a deterministic Fabric transaction execution context for unit testing.
 * <p>
 * This annotation supplies all identity, proposal, and invocation metadata required
 * to construct a {@link ChaincodeStub} without involving Fabric runtime components.
 * Defaults represent a fully valid, self-contained mock transaction and may be
 * selectively overridden per test.
 */
@Ledger
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@ExtendWith(StubExtension.class)
@Inherited
public @interface Stub {

    String DEFAULT_MSP_ID = "MockMSP";

    String DEFAULT_CERT = """
            -----BEGIN CERTIFICATE-----
            MIIB3jCCAYOgAwIBAgIQJ/u7Hugy4jCGolNuNPaA3jAKBggqhkjOPQQDAjASMRAw
            DgYDVQQDEwdNb2NrIENBMB4XDTI1MTIwOTA2NTYzMloXDTM1MTIwNzA2NTYzMlow
            EjEQMA4GA1UEAxMHTW9jayBDQTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABF7E
            GBoY1PYtwYdIqx472C7nzN1tLgHpKJf0U7oGRkegl07salHhqC+ccZewGILlGHQM
            5EwP50CCV2JxKc9P0zyjgbowgbcwDgYDVR0PAQH/BAQDAgGmMB0GA1UdJQQWMBQG
            CCsGAQUFBwMCBggrBgEFBQcDATAPBgNVHRMBAf8EBTADAQH/MCkGA1UdDgQiBCB8
            zD3TeBxbtOvRobhZD+rUfYG8wiMunTfUPtYr2dp9fzBKBgNVHREEQzBBghIqLjEy
            Ny0wLTAtMS5uaXAuaW+CCTEyNy4wLjAuMYIJbG9jYWxob3N0ggcwLjAuMC4wggwq
            LmxvY2FsaG8uc3QwCgYIKoZIzj0EAwIDSQAwRgIhAJKRIM50TVxLjWBvv4RS6jHH
            wSBTXGPq0leEDQS+DZzdAiEA4lKTXaeHAByAJvR0H3Mq9/qo6lZwKn+jFOFTslrL
            +XM=
            -----END CERTIFICATE-----
            """;

    String DEFAULT_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgEBHTZpSRAzJUZLIF
            z4mq4gm8QwEiuVK/USaTZlEPW1GhRANCAAQ/1W0DhUjH7mW+ry1oYAT+pHSgOwIP
            Nec2V6G9vdPKOxIqontufC0cUoxBACvWoRX9Fg4CCL96PiYFh5kbAUok
            -----END PRIVATE KEY-----
            """;

    String DEFAULT_CHANNEL = "mock-channel";

    String DEFAULT_FUNCTION = "mock-function";

    long DEFAULT_TIMESTAMP_MILLIS = 578_493_030_555L;

    /**
     * MSP identifier of the transaction creator.
     * <p>
     * This value represents the organizational identity under which the proposal is evaluated.
     */
    String mspId() default DEFAULT_MSP_ID;

    /**
     * PEM-encoded Fabric enrollment certificate (e-cert) of the transaction creator.
     * <p>
     * This certificate is embedded in the signed proposal and exposed via
     * {@link ChaincodeStub#getCreator()}.
     *
     * <pre>{@code
     * Certificate:
     *     Data:
     *         Version: 3 (0x2)
     *         Serial Number:
     *             db:df:6a:77:46:e0:b5:a1:48:2d:50:12:a1:95:9b:80
     *         Signature Algorithm: ecdsa-with-SHA256
     *         Issuer: CN = Mock CA
     *         Validity
     *             Not Before: Dec  9 06:56:32 2025 GMT
     *             Not After : Dec  7 06:56:32 2035 GMT
     *         Subject: OU = admin, CN = Mock Admin
     *         Subject Public Key Info:
     *             Public Key Algorithm: id-ecPublicKey
     *                 Public-Key: (256 bit)
     *                 pub:
     *                     04:3f:d5:6d:03:85:48:c7:ee:65:be:af:2d:68:60:
     *                     04:fe:a4:74:a0:3b:02:0f:35:e7:36:57:a1:bd:bd:
     *                     d3:ca:3b:12:2a:a2:7b:6e:7c:2d:1c:52:8c:41:00:
     *                     2b:d6:a1:15:fd:16:0e:02:08:bf:7a:3e:26:05:87:
     *                     99:1b:01:4a:24
     *                 ASN1 OID: prime256v1
     *                 NIST CURVE: P-256
     *         X509v3 extensions:
     *             X509v3 Key Usage: critical
     *                 Digital Signature, Key Encipherment
     *             X509v3 Extended Key Usage:
     *                 TLS Web Client Authentication, TLS Web Server Authentication
     *             X509v3 Basic Constraints: critical
     *                 CA:FALSE
     *             X509v3 Subject Key Identifier:
     *                 2B:59:71:36:25:F5:A9:A3:11:18:F9:78:07:0D:92:A3:12:DA:A0:02:56:D9:5B:51:F2:BF:7E:C6:A7:4A:93:79
     *             X509v3 Authority Key Identifier:
     *                 7C:CC:3D:D3:78:1C:5B:B4:EB:D1:A1:B8:59:0F:EA:D4:7D:81:BC:C2:23:2E:9D:37:D4:3E:D6:2B:D9:DA:7D:7F
     *             X509v3 Subject Alternative Name:
     *                 DNS:*.127-0-0-1.nip.io, DNS:127.0.0.1, DNS:localhost, DNS:0.0.0.0, DNS:*.localho.st
     *     Signature Algorithm: ecdsa-with-SHA256
     *     Signature Value:
     *         30:44:02:20:2d:68:2a:a6:f0:e9:a3:2a:84:fb:65:fd:43:41:
     *         7b:0a:cc:62:92:20:f2:d6:2d:b5:de:d2:13:36:7c:dc:f4:a1:
     *         02:20:66:60:3d:e0:c5:a7:d7:5c:ab:a5:88:cb:ac:bd:9d:1e:
     *         45:03:1b:51:67:ce:46:86:48:e7:e3:96:d2:85:fe:74
     * }</pre>
     */
    String cert() default DEFAULT_CERT;

    /**
     * PEM-encoded private key of the transaction creator.
     * <p>
     * This key is used exclusively to calculate the proposal signature during
     * mock {@link SignedProposal} construction. It has no persistence or runtime
     * side effects beyond signature generation.
     *
     * <pre>{@code
     * Private-Key: (256 bit)
     * priv:
     *     10:11:d3:66:94:91:03:32:54:64:b2:05:cf:89:aa:
     *     e2:09:bc:43:01:22:b9:52:bf:51:26:93:66:51:0f:
     *     5b:51
     * pub:
     *     04:3f:d5:6d:03:85:48:c7:ee:65:be:af:2d:68:60:
     *     04:fe:a4:74:a0:3b:02:0f:35:e7:36:57:a1:bd:bd:
     *     d3:ca:3b:12:2a:a2:7b:6e:7c:2d:1c:52:8c:41:00:
     *     2b:d6:a1:15:fd:16:0e:02:08:bf:7a:3e:26:05:87:
     *     99:1b:01:4a:24
     * ASN1 OID: prime256v1
     * NIST CURVE: P-256
     * }</pre>
     */
    String key() default DEFAULT_KEY;

    /**
     * Chaincode metadata associated with the invocation.
     * <p>
     * This information populates the proposal header and identifies the logical
     * chaincode target during execution.
     */
    Chaincode chaincode() default @Chaincode();

    /**
     * Name of the channel on which the chaincode is invoked.
     * <p>
     * This value is returned by {@link ChaincodeStub#getChannelId()} and participates
     * in proposal header construction.
     */
    String channel() default DEFAULT_CHANNEL;

    /**
     * Name of the chaincode function being invoked.
     * <p>
     * This corresponds to the first argument in {@link ChaincodeStub#getStringArgs()}.
     */
    String function() default DEFAULT_FUNCTION;

    /**
     * Chaincode function arguments excluding the context parameter.
     * <p>
     * If the target method accepts only a context parameter, this array is empty.
     * Otherwise, values are passed in order following the function name.
     * <p>
     * These correspond to the argument in {@link ChaincodeStub#getStringArgs()} except first one.
     */
    String[] args() default {};

    /**
     * Transaction transient data expressed as CSV entries.
     * <p>
     * Each element must follow {@code "key,value"} format. Entries are split and
     * materialized into the transient map exposed by
     * {@link ChaincodeStub#getTransient()}.
     *
     * <pre>{@code
     * { "key1,value1", "key2,value2" }
     * }</pre>
     */
    String[] transients() default {};

    /**
     * Transaction timestamp expressed as milliseconds since the Unix epoch.
     * <p>
     * This value is converted into a Fabric {@code Timestamp} and exposed via
     * {@link org.hyperledger.fabric.shim.ChaincodeStub#getTxTimestamp()} during
     * chaincode execution.
     * <p>
     * The default corresponds to the instant:
     * {@code 1988-05-01T12:30:30.555Z}.
     * <p>
     * If this value is left unchanged, the mock infrastructure will ignore
     * the constant and instead use the current system time at stub creation,
     * providing realistic, non-deterministic timestamps.
     * <p>
     * Supplying an explicit value enables deterministic testing of
     * time-dependent chaincode logic such as validation rules, temporal
     * constraints, and audit metadata.
     */
    long timestamp() default 578_493_030_555L;

    /**
     * Nonce value used when constructing the Fabric {@code SignatureHeader}
     * for the mocked transaction.
     * <p>
     * In Hyperledger Fabric, the nonce is combined with the creator identity
     * to derive the transaction ID, ensuring uniqueness and replay protection.
     * <p>
     * If this value is left unchanged (all-zero default), the mock runtime
     * will generate a cryptographically random nonce for each invocation,
     * closely mirroring real Fabric behavior.
     * <p>
     * Providing a custom nonce forces deterministic transaction ID
     * generation, which is particularly useful for repeatable tests and
     * precise history assertions.
     *
     * @see org.hyperledger.fabric.shim.ChaincodeStub#getTxId()
     */
    byte[] nonce() default {
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0
    };

    /**
     * Chaincode identification metadata.
     */
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface Chaincode {

        String DEFAULT_CC_NAME = "mock-cc";

        String DEFAULT_CC_VERSION = "0.0.1";

        /**
         * Logical name of the chaincode.
         */
        String name() default DEFAULT_CC_NAME;

        /**
         * Version identifier of the chaincode.
         */
        String version() default DEFAULT_CC_VERSION;
    }
}
