package ir.co.isc.spbp.blockchain.fabino;

import ir.co.isc.spbp.blockchain.fabino.builder.CcBuilder;
import ir.co.isc.spbp.blockchain.fabino.builder.CcaasBuilder;
import lombok.Getter;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Proxy;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@code @Microfab} provides an annotation-based configuration for
 * running a Microfab network in tests. <br><br>
 * <p>
 * This annotation is a Java DSL mirror of the
 * <a href="https://github.com/hyperledger-labs/microfab/blob/main/internal/app/microfabd/config.go">Microfab configuration struct</a>.
 * It allows developers to configure organizations, channels, TLS,
 * CouchDB, CAs, and other runtime settings directly in code.
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @Microfab(
 *     image = "hyperledger-labs/microfab:latest",
 *     domain = "127-0-0-1.nip.io",
 *     port = 8080,
 *     directory = "/home/microfab/data",
 *     orderingOrganization = @Microfab.Organization(name = "Orderer"),
 *     endorsingOrganizations = {
 *         @Microfab.Organization(name = "Org1"),
 *         @Microfab.Organization(name = "Org2")
 *     },
 *     channels = {
 *         @Microfab.Channel(
 *             name = "mychannel",
 *             endorsingOrganizations = {"Org1", "Org2"},
 *             capabilityLevel = "V2_0"
 *         )
 *     },
 *     capabilityLevel = "V2_5",
 *     couchdb = true,
 *     certificateAuthorities = true,
 *     timeout = "60s",
 *     tls = @Microfab.Tls(enabled = true)
 * )
 * public class MyTest { }
 * }</pre>
 */
@Target(TYPE)
@Retention(RUNTIME)
@ExtendWith(MicrofabExtension.class)
@Inherited
public @interface Microfab {

    /**
     * The Docker image used to run Microfab.
     * <p>
     * Default: {@code "hyperledger-labs/microfab:latest"}
     */
    String image() default "hyperledger-labs/microfab:latest";

    /**
     * Domain used by Microfab for hostnames of peers, orderers, and CAs.
     * <p>
     * Default: {@code "127-0-0-1.nip.io"} (which maps all subdomains to localhost).
     */
    String domain() default "127-0-0-1.nip.io";

    /**
     * REST API port exposed by Microfab for network management.
     * <p>
     * Default: {@code 8080}.
     */
    int port() default 8080;

    /**
     * Filesystem directory where Microfab persists state (e.g., ledger data).
     * <p>
     * Default: {@code "/home/microfab/data"}.
     */
    String directory() default "/home/microfab/data";

    /**
     * Defines the ordering organization for the network.
     * <p>
     * Default: {@code @Organization(name = "Orderer")}.
     */
    Organization orderingOrganization() default @Organization(name = "Orderer");

    /**
     * List of endorsing organizations in the network.
     * <p>
     * Default: one organization {@code Org1}.
     */
    Organization[] endorsingOrganizations() default @Organization(name = "Org1");

    /**
     * Channels to be created in the network.
     * <p>
     * Default: one channel {@code channel1} with {@code Org1} as an endorser.
     */
    Channel[] channels() default {
            @Channel(
                    name = "channel1",
                    endorsingOrganizations = {"Org1"}
            )
    };

    /**
     * Fabric capability level for the network (applies to channels if not overridden).
     * <p>
     * Common values: {@code V2_0}, {@code V2_5}.
     * <p>
     * Default: {@code "V2_5"}.
     */
    String capabilityLevel() default "V2_5";

    /**
     * Whether CouchDB should be used as the state database for peers.
     * <p>
     * Default: {@code true}.
     */
    boolean couchdb() default true;

    /**
     * Whether Fabric Certificate Authorities (CAs) should be deployed for each organization.
     * <p>
     * Default: {@code true}.
     */
    boolean certificateAuthorities() default true;

    /**
     * Timeout for network operations, expressed as a duration string
     * (e.g., {@code "30s"}, {@code "1m"}).
     * <p>
     * Default: {@code "30s"}.
     */
    String timeout() default "30s";

    /**
     * TLS configuration for network components.
     * <p>
     * Default: TLS disabled.
     */
    Tls tls() default @Tls();

    /**
     * Registry of chaincodes that are available to this Microfab network.
     * <p>
     * Declaring chaincodes here makes them known to the Microfab runtime
     * but does <b>not</b> automatically deploy them to any channel. The
     * chaincodes listed here act as global definitions that can be reused
     * across multiple channels.
     * </p>
     *
     * <p>
     * To actually install and initialize a chaincode, it must also be
     * referenced in the {@link Channel#chaincodes()} property of a
     * {@link Channel} definition. This allows you to declare chaincodes
     * once and then selectively deploy them to one or more channels.
     * </p>
     *
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * @Microfab(
     *     chaincodes = {
     *         @Chaincode(name = "asset-transfer", version = "1.0"),
     *         @Chaincode(name = "supplychain", version = "2.0", type = Chaincode.Type.CCAAS)
     *     },
     *     channels = {
     *         name = "mychannel",
     *         chaincodes = {
     *             "asset-transfer", // deployed
     *             "supplychain"     // deployed
     *         }
     *
     *     }
     * )
     * public class MyTest {
     * }
     * }</pre>
     *
     * <p>
     * In the above example, two chaincodes are declared at the Microfab
     * level but only deployed once they are referenced in the channel.
     * This design mirrors Fabric's lifecycle model, where chaincodes are
     * packaged and approved before they are actually committed to a
     * channel.
     * </p>
     *
     * @return array of globally declared {@link Chaincode} definitions.
     * Defaults to an empty array.
     */
    Chaincode[] chaincodes() default {};

    // ------------------------------------------------------
    // Nested annotations
    // ------------------------------------------------------

    /**
     * Defines an organization within the Microfab network.
     */
    @Target({})
    @Retention(RUNTIME)
    @interface Organization {

        /**
         * The name of the organization (e.g., {@code Org1}, {@code Org2}, {@code Orderer}).
         */
        String name();
    }

    /**
     * Defines a channel within the Microfab network.
     */
    @Target({})
    @Retention(RUNTIME)
    @interface Channel {

        /**
         * The name of the channel (e.g., {@code mychannel}, {@code tokenization}).
         */
        String name();

        /**
         * Names of organizations that can endorse transactions on this channel.
         */
        String[] endorsingOrganizations();

        /**
         * Optional capability level override for this channel.
         * <p>
         * If empty, the top-level {@link Microfab#capabilityLevel()} is used.
         */
        String capabilityLevel() default "";

        /**
         * List of chaincode names to be installed and committed on this channel.
         * <p>
         * The names supplied here must correspond to chaincodes previously
         * declared at the {@link Microfab#chaincodes()} level. Declaring a
         * chaincode globally in {@code @Microfab} makes it available to the
         * network, while referencing its name here installs and instantiates
         * it on the specific channel.
         * </p>
         *
         * <p>
         * This design mirrors the Fabric lifecycle model:
         * <ul>
         *     <li><b>Global declaration</b> – a chaincode is packaged and
         *         registered at the network level via {@code @Microfab}.</li>
         *     <li><b>Channel deployment</b> – the chaincode is explicitly
         *         bound to a channel when listed in this property.</li>
         * </ul>
         * </p>
         *
         * @return array of chaincode names to be deployed on this channel.
         * Defaults to an empty array.
         */
        String[] chaincodes() default {};
    }

    /**
     * TLS configuration for peers, orderers, and CAs.
     * <p>
     * This annotation allows configuring TLS for Hyperledger Fabric components.
     * When TLS is enabled, you must provide certificates and keys in PEM format.
     * <p>
     * <b>Example: Generate and configure TLS certificates on Linux using OpenSSL</b>
     * <p>
     * <b>1. Create a CA key and certificate:</b>
     * <pre>{@code
     * # Generate a CA private key (PKCS#8 format)
     * openssl ecparam -name prime256v1 -genkey -noout | openssl pkcs8 -topk8 -nocrypt -out ca.key
     *
     * # Generate a self-signed CA certificate valid for 10 years
     * openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 \
     *     -out ca.crt \
     *     -subj "/C=IR/ST=Tehran/L=Tehran/O=Fabino/OU=Test/CN=Microfab TLS CA"
     *
     * # Remove trailing newline and encode in Base64 (important for Go compatibility)
     * sed -z 's/\n$//' ca.crt | base64 -w0
     * }</pre>
     * <p>
     * <b>2. Create TLS key and CSR:</b>
     * <pre>{@code
     * # Generate TLS private key
     * openssl ecparam -name prime256v1 -genkey -noout | openssl pkcs8 -topk8 -nocrypt -out tls.key
     *
     * # Create a CSR for TLS certificate
     * openssl req -new -key tls.key -out tls.csr \
     *     -subj "/C=IR/ST=Tehran/L=Tehran/O=Fabino/OU=Test/CN=Microfab TLS"
     *
     * # Remove trailing newline and encode TLS key in Base64
     * sed -z 's/\n$//' tls.key | base64 -w0
     * }</pre>
     * <p>
     * <b>3. Sign TLS certificate with CA:</b>
     * <pre>{@code
     * # Sign TLS certificate with CA
     * openssl x509 -req -in tls.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
     *     -out tls.crt -days 3650 -sha256 -extfile server-ext.cnf
     *
     * # Remove trailing newline and encode TLS certificate in Base64
     * sed -z 's/\n$//' tls.crt | base64 -w0
     * }</pre>
     * <p>
     * <b>4. Example server-ext.cnf content (for SAN / DNS names):</b>
     * <pre>{@code
     * authorityKeyIdentifier=keyid,issuer
     * basicConstraints=CA:FALSE
     * keyUsage = digitalSignature, keyEncipherment
     * extendedKeyUsage=serverAuth, clientAuth
     * subjectAltName = @alt_names
     *
     * [alt_names]
     * DNS.1 = *.127-0-0-1.nip.io
     * DNS.2 = 127.0.0.1
     * DNS.3 = localhost
     * DNS.4 = 0.0.0.0
     * DNS.5 = *.localho.st
     * # You can add additional DNS records based on your domain
     * #DNS.6 = *.172-29-0-5.nip.io
     * #DNS.7 = *.example.com
     * }</pre>
     * <p>
     * <b>Important:</b> Always remove the trailing newline from PEM files before Base64 encoding.
     * If you see {@code Cg==} at the end of your Base64-encoded value, it means the PEM file
     * contains a newline character. Microfab cannot work with Base64 values that include a
     * trailing newline, so make sure to remove it, e.g. using:
     * <pre>{@code
     * sed -z 's/\n$//' file.pem | base64 -w0
     * }</pre>
     *
     * @see <a href="https://www.openssl.org/docs/man1.1.1/man1/openssl-req.html">openssl req manual</a>
     * @see <a href="https://www.openssl.org/docs/man1.1.1/man1/openssl-x509.html">openssl x509 manual</a>
     */
    @Target({})
    @Retention(RUNTIME)
    @interface Tls {

        /**
         * Whether TLS is enabled.
         * <p>
         * Default: {@code false}.
         */
        boolean enabled() default false;

        /**
         * Certificate in PEM format, Base64 encoded.
         * <p>
         * Default: empty (no certificate).
         * <p>
         *
         * <p>This property defines the TLS certificate presented by the peer, orderer, or client during
         * mutual TLS authentication. The value must be a Base64-encoded representation of a PEM certificate.
         * Ensure that any trailing newline in the original PEM file is removed before encoding — if present,
         * it will result in {@code Cg==} at the end of the Base64 output, which can break parsing in
         * Go-based systems such as Microfab.</p>
         *
         * <h3>Example Certificate (PEM format)</h3>
         * <pre>{@code
         * -----BEGIN CERTIFICATE-----
         * MIIClzCCAj6gAwIBAgIUbaAq2werSRns57U5imprKNYe1UkwCgYIKoZIzj0EAwIw
         * aTELMAkGA1UEBhMCSVIxDzANBgNVBAgMBlRlaHJhbjEPMA0GA1UEBwwGVGVocmFu
         * MQ8wDQYDVQQKDAZGYWJpbm8xDTALBgNVBAsMBFRlc3QxGDAWBgNVBAMMD01pY3Jv
         * ZmFiIFRMUyBDQTAeFw0yNTEwMTEwNTQyMDdaFw0zNTEwMDkwNTQyMDdaMGYxCzAJ
         * BgNVBAYTAklSMQ8wDQYDVQQIDAZUZWhyYW4xDzANBgNVBAcMBlRlaHJhbjEPMA0G
         * A1UECgwGRmFiaW5vMQ0wCwYDVQQLDARUZXN0MRUwEwYDVQQDDAxNaWNyb2ZhYiBU
         * TFMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATni6+AM3ZOKuwJP4QskZ26VCCY
         * 2L5PrqWNzouD87MhVWXtYfnS7qtDek7km7q50rtuQpzU5w8zx/Y1OwvfEbe9o4HG
         * MIHDMB8GA1UdIwQYMBaAFOCV3DFykbtow3r9Co3NyxLaPkSTMAkGA1UdEwQCMAAw
         * CwYDVR0PBAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBKBgNV
         * HREEQzBBghIqLjEyNy0wLTAtMS5uaXAuaW+CCTEyNy4wLjAuMYIJbG9jYWxob3N0
         * ggcwLjAuMC4wggwqLmxvY2FsaG8uc3QwHQYDVR0OBBYEFB0AppMtXxdEvohWJKNg
         * KMK8vMecMAoGCCqGSM49BAMCA0cAMEQCIAYZJpitxM+ENLJ7wJVkIR7uq8Bxk6zW
         * v07so0UeYWUnAiAvHR2TZal1acCVVHkjsInk3gA1Z3ILU7qexo00TWPCmA==
         * -----END CERTIFICATE-----
         * }</pre>
         *
         * <p>To encode this PEM certificate to Base64 without appending a newline at the end, you can use:</p>
         * <pre>{@code
         * sed -z 's/\n$//' tls.crt | base64 -w0
         * }</pre>
         *
         * <h3>Example Base64-Encoded Output</h3>
         * <pre>{@code
         * LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNsekNDQWo2Z0F3SUJBZ0lV
         * YmFBcTJ3ZXJTUm5zNTdVNWltcHJLTlllMVVrd0NnWUlLb1pJemowRUF3SXcKYVRF
         * TE1Ba0dBMVVFQmhNQ1NWSXhEekFOQmdOVkJBZ01CbFJsYUhKaGJqRVBNQTBHQTFV
         * RUJ3d0dWR1ZvY21GdQpNUTh3RFFZRFZRUUtEQVpHWVdKcGJtOHhEVEFMQmdOVkJB
         * c01CRlJsYzNReEdEQVdCZ05WQkFNTUQwMXBZM0p2ClptRmlJRlJNVXlCRFFUQWVG
         * dzB5TlRFd01URXdOVFF5TURkYUZ3MHpOVEV3TURrd05UUXlNRGRhTUdZeEN6QUoK
         * QmdOVkJBWVRBa2xTTVE4d0RRWURWUVFJREFaVVpXaHlZVzR4RHpBTkJnTlZCQWNN
         * QmxSbGFISmhiakVQTUEwRwpBMVVFQ2d3R1JtRmlhVzV2TVEwd0N3WURWUVFMREFS
         * VVpYTjBNUlV3RXdZRFZRUUREQXhOYVdOeWIyWmhZaUJVClRGTXdXVEFUQmdjcWhr
         * ak9QUUlCQmdncWhrak9QUU1CQndOQ0FBVG5pNitBTTNaT0t1d0pQNFFza1oyNlZD
         * Q1kKMkw1UHJxV056b3VEODdNaFZXWHRZZm5TN3F0RGVrN2ttN3E1MHJ0dVFwelU1
         * dzh6eC9ZMU93dmZFYmU5bzRIRwpNSUhETUI4R0ExVWRJd1FZTUJhQUZPQ1YzREZ5
         * a2J0b3czcjlDbzNOeXhMYVBrU1RNQWtHQTFVZEV3UUNNQUF3CkN3WURWUjBQQkFR
         * REFnV2dNQjBHQTFVZEpRUVdNQlFHQ0NzR0FRVUZCd01CQmdnckJnRUZCUWNEQWpC
         * S0JnTlYKSFJFRVF6QkJnaElxTGpFeU55MHdMVEF0TVM1dWFYQXVhVytDQ1RFeU55
         * NHdMakF1TVlJSmJHOWpZV3hvYjNOMApnZ2N3TGpBdU1DNHdnZ3dxTG14dlkyRnNh
         * Rzh1YzNRd0hRWURWUjBPQkJZRUZCMEFwcE10WHhkRXZvaFdKS05nCktNSzh2TWVj
         * TUFvR0NDcUdTTTQ5QkFNQ0EwY0FNRVFDSUFZWkpwaXR4TStFTkxKN3dKVmtJUjd1
         * cThCeGs2elcKdjA3c28wVWVZV1VuQWlBdkhSMlRaYWwxYWNDVlZIa2pzSW5rM2dB
         * MVozSUxVN3FleG8wMFRXUENtQT09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0=
         * }</pre>
         *
         * <p><b>⚠️ Important:</b> If your Base64-encoded string ends with {@code Cg==},
         * it indicates that the original PEM file contained a trailing newline.
         * You must remove it before encoding, otherwise Go-based TLS clients and servers (such as Microfab)
         * will fail to parse it properly.</p>
         *
         * <h3>Java Example</h3>
         * <pre>{@code
         * @Tls(
         *     enabled = true,
         *     certificate = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNsekN" +
         *                   "DQWo2Z0F3SUJBZ0lVYmFBcTJ3ZXJTUm5zNTdVNWltcHJLTl" +
         *                   "llMVVrd0NnWUlLb1pJemowRUF3SXcKYVRFTE1Ba0dBMVVFQ" +
         *                   "mhNQ1NWSXhEekFOQmdOVkJBZ01CbFJsYUhKaGJqRVBNQTBH" +
         *                   "QTFVRUJ3d0dWR1ZvY21GdQpNUTh3RFFZRFZRUUtEQVpHWVd" +
         *                   "KcGJtOHhEVEFMQmdOVkJBc01CRlJsYzNReEdEQVdCZ05WQk" +
         *                   "FNTUQwMXBZM0p2ClptRmlJRlJNVXlCRFFUQWVGdzB5TlRFd" +
         *                   "01URXdOVFF5TURkYUZ3MHpOVEV3TURrd05UUXlNRGRhTUdZ" +
         *                   "eEN6QUoKQmdOVkJBWVRBa2xTTVE4d0RRWURWUVFJREFaVVp" +
         *                   "XaHlZVzR4RHpBTkJnTlZCQWNNQmxSbGFISmhiakVQTUEwRw" +
         *                   "pBMVVFQ2d3R1JtRmlhVzV2TVEwd0N3WURWUVFMREFSVVpYT" +
         *                   "jBNUlV3RXdZRFZRUUREQXhOYVdOeWIyWmhZaUJVClRGTXdX" +
         *                   "VEFUQmdjcWhrak9QUUlCQmdncWhrak9QUU1CQndOQ0FBVG5" +
         *                   "pNitBTTNaT0t1d0pQNFFza1oyNlZDQ1kKMkw1UHJxV056b3" +
         *                   "VEODdNaFZXWHRZZm5TN3F0RGVrN2ttN3E1MHJ0dVFwelU1d" +
         *                   "zh6eC9ZMU93dmZFYmU5bzRIRwpNSUhETUI4R0ExVWRJd1FZ" +
         *                   "TUJhQUZPQ1YzREZ5a2J0b3czcjlDbzNOeXhMYVBrU1RNQWt" +
         *                   "HQTFVZEV3UUNNQUF3CkN3WURWUjBQQkFRREFnV2dNQjBHQT" +
         *                   "FVZEpRUVdNQlFHQ0NzR0FRVUZCd01CQmdnckJnRUZCUWNEQ" +
         *                   "WpCS0JnTlYKSFJFRVF6QkJnaElxTGpFeU55MHdMVEF0TVM1" +
         *                   "dWFYQXVhVytDQ1RFeU55NHdMakF1TVlJSmJHOWpZV3hvYjN" +
         *                   "OMApnZ2N3TGpBdU1DNHdnZ3dxTG14dlkyRnNhRzh1YzNRd0" +
         *                   "hRWURWUjBPQkJZRUZCMEFwcE10WHhkRXZvaFdKS05nCktNS" +
         *                   "zh2TWVjTUFvR0NDcUdTTTQ5QkFNQ0EwY0FNRVFDSUFZWkpw" +
         *                   "aXR4TStFTkxKN3dKVmtJUjd1cThCeGs2elcKdjA3c28wVWV" +
         *                   "ZV1VuQWlBdkhSMlRaYWwxYWNDVlZIa2pzSW5rM2dBMVozSU" +
         *                   "xVN3FleG8wMFRXUENtQT09Ci0tLS0tRU5EIENFUlRJRklDQ" +
         *                   "VRFLS0tLS0="
         * )
         * }</pre>
         */
        String certificate() default "";

        /**
         * TLS private key in PEM format, Base64 encoded.
         * <p>
         * Default: empty (no private key).
         * <p>
         *
         * <p>This property defines the private key used for the TLS certificate of the node or client.
         * The value must be a Base64-encoded representation of a PEM private key. Ensure that the PEM
         * file does not contain a trailing newline before encoding it — otherwise the resulting Base64
         * will end with {@code Cg==}, indicating an extra newline at the end. Go-based components (such
         * as Microfab) cannot correctly parse Base64-encoded private keys that include this trailing
         * newline.</p>
         *
         * <p><b>⚠️ Hint:</b> Microfab uses <b>ECDSA keys</b> (not RSA) for TLS private keys. See the
         * implementation here:
         * <a href="https://github.com/hyperledger-labs/microfab/blob/main/internal/pkg/identity/privatekey/privatekey.go">
         * Microfab ECDSA private key parsing</a>.</p>
         *
         * <h3>Example Private Key (PEM format)</h3>
         * <pre>{@code
         * -----BEGIN PRIVATE KEY-----
         * MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgrHzEk7Z+PKMLl1JU
         * mdKSLPSiIlMMgjsicEBKN4ypyB+hRANCAATni6+AM3ZOKuwJP4QskZ26VCCY2L5P
         * rqWNzouD87MhVWXtYfnS7qtDek7km7q50rtuQpzU5w8zx/Y1OwvfEbe9
         * -----END PRIVATE KEY-----
         * }</pre>
         *
         * <p>To safely Base64-encode this PEM file without appending a newline at the end, use:</p>
         * <pre>{@code
         * sed -z 's/\n$//' tls.key | base64 -w0
         * }</pre>
         *
         * <h3>Example Base64-Encoded Output</h3>
         * <pre>{@code
         * LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JR0hBZ0VBTUJNR0J5cUdTTTQ5
         * QWdFR0NDcUdTTTQ5QXdFSEJHMHdhd0lCQVFRZ3JIekVrN1orUEtNTGwxSlUKbWRL
         * U0xQU2lJbE1NZ2pzaWNFQktONHlweUIraFJBTkNBQVRuaTYrQU0zWk9LdXdKUDRR
         * c2taMjZWQ0NZMkw1UApycVdOem91RDg3TWhWV1h0WWZuUzdxdERlazdrbTdxNTBy
         * dHVRcHpVNXc4engvWTFPd3ZmRWJlOQotLS0tLUVORCBQUklWQVRFIEtFWS0tLS0t
         * }</pre>
         *
         * <h3>Java Example</h3>
         * <pre>{@code
         * @Tls(
         *     enabled = true,
         *     privateKey = "LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JR0hBZ0" +
         *                  "VBTUJNR0J5cUdTTTQ5QWdFR0NDcUdTTTQ5QXdFSEJHMHdh" +
         *                  "d0lCQVFRZ3JIekVrN1orUEtNTGwxSlUKbWRLU0xQU2lJbE" +
         *                  "1NZ2pzaWNFQktONHlweUIraFJBTkNBQVRuaTYrQU0zWk9L" +
         *                  "dXdKUDRRc2taMjZWQ0NZMkw1UApycVdOem91RDg3TWhWV1" +
         *                  "h0WWZuUzdxdERlazdrbTdxNTBydHVRcHpVNXc4engvWTFP" +
         *                  "d3ZmRWJlOQotLS0tLUVORCBQUklWQVRFIEtFWS0tLS0t"
         * )
         * }</pre>
         */
        String privateKey() default "";

        /**
         * CA certificate in PEM format, Base64 encoded.
         * <p>
         * Default: empty (no CA).
         * <p>
         *
         * <p>This property defines the trusted CA certificate used to verify the TLS certificate of peers,
         * orderers, or clients. The value must be a Base64-encoded representation of a PEM certificate.
         * Make sure to remove any trailing newline before encoding — a trailing newline will result in
         * {@code Cg==} at the end of the Base64 output, indicating that the PEM file contains a newline.
         * Go-based components (such as Microfab) cannot correctly parse Base64-encoded PEM values that
         * include trailing newlines.</p>
         *
         * <h3>Example CA Certificate (PEM format)</h3>
         * <pre>{@code
         * -----BEGIN CERTIFICATE-----
         * MIICKDCCAc2gAwIBAgIUT/Ccm0n+lL6yOpmrLrPXrPXATmUwCgYIKoZIzj0EAwIw
         * aTELMAkGA1UEBhMCSVIxDzANBgNVBAgMBlRlaHJhbjEPMA0GA1UEBwwGVGVocmFu
         * MQ8wDQYDVQQKDAZGYWJpbm8xDTALBgNVBAsMBFRlc3QxGDAWBgNVBAMMD01pY3Jv
         * ZmFiIFRMUyBDQTAeFw0yNTEwMTEwNTQxNDRaFw0zNTEwMDkwNTQxNDRaMGkxCzAJ
         * BgNVBAYTAklSMQ8wDQYDVQQIDAZUZWhyYW4xDzANBgNVBAcMBlRlaHJhbjEPMA0G
         * A1UECgwGRmFiaW5vMQ0wCwYDVQQLDARUZXN0MRgwFgYDVQQDDA9NaWNyb2ZhYiBU
         * TFMgQ0EwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARUO6z11VsKqIqtPThuQ5Wv
         * 52h+xyx/3MNtIVQfbIsJ6LbwTQpNn5b7s4cX6STcHLlAp+Nhnr1ylTZCHHlVyF17
         * o1MwUTAdBgNVHQ4EFgQU4JXcMXKRu2jDev0Kjc3LEto+RJMwHwYDVR0jBBgwFoAU
         * 4JXcMXKRu2jDev0Kjc3LEto+RJMwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQD
         * AgNJADBGAiEAwzXqHCn8OBeN8bv6sTwi4ElSjVHji+YWQNVLnhV1TEsCIQCiegmh
         * SHzu4OyIdY2U/x0478hopJgJEUMb+fMWprbaAg==
         * -----END CERTIFICATE-----
         * }</pre>
         *
         * <p>To encode this PEM certificate to Base64 without appending a newline at the end, you can use:</p>
         * <pre>{@code
         * sed -z 's/\n$//' ca.crt | base64 -w0
         * }</pre>
         *
         * <h3>Example Base64-Encoded Output</h3>
         * <pre>{@code
         * LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNLRENDQWMyZ0F3SUJBZ0lV
         * VC9DY20wbitsTDZ5T3BtckxyUFhyUFhBVG1Vd0NnWUlLb1pJemowRUF3SXcKYVRF
         * TE1Ba0dBMVVFQmhNQ1NWSXhEekFOQmdOVkJBZ01CbFJsYUhKaGJqRVBNQTBHQTFV
         * RUJ3d0dWR1ZvY21GdQpNUTh3RFFZRFZRUUtEQVpHWVdKcGJtOHhEVEFMQmdOVkJB
         * c01CRlJsYzNReEdEQVdCZ05WQkFNTUQwMXBZM0p2ClptRmlJRlJNVXlCRFFUQWVG
         * dzB5TlRFd01URXdOVFF4TkRSYUZ3MHpOVEV3TURrd05UUXhORFJhTUdreEN6QUoK
         * QmdOVkJBWVRBa2xTTVE4d0RRWURWUVFJREFaVVpXaHlZVzR4RHpBTkJnTlZCQWNN
         * QmxSbGFISmhiakVQTUEwRwpBMVVFQ2d3R1JtRmlhVzV2TVEwd0N3WURWUVFMREFS
         * VVpYTjBNUmd3RmdZRFZRUUREQTlOYVdOeWIyWmhZaUJVClRGTWdRMEV3V1RBVEJn
         * Y3Foa2pPUFFJQkJnZ3Foa2pPUFFNQkJ3TkNBQVJVTzZ6MTFWc0txSXF0UFRodVE1
         * V3YKNTJoK3h5eC8zTU50SVZRZmJJc0o2TGJ3VFFwTm41YjdzNGNYNlNUY0hMbEFw
         * K05obnIxeWxUWkNISGxWeUYxNwpvMU13VVRBZEJnTlZIUTRFRmdRVTRKWGNNWEtS
         * dTJqRGV2MEtqYzNMRXRvK1JKTXdId1lEVlIwakJCZ3dGb0FVCjRKWGNNWEtSdTJq
         * RGV2MEtqYzNMRXRvK1JKTXdEd1lEVlIwVEFRSC9CQVV3QXdFQi96QUtCZ2dxaGtq
         * T1BRUUQKQWdOSkFEQkdBaUVBd3pYcUhDbjhPQmVOOGJ2NnNUd2k0RWxTalZIamkr
         * WVdRTlZMbmhWMVRFc0NJUUNpZWdtaApTSHp1NE95SWRZMlUveDA0Nzhob3BKZ0pF
         * VU1iK2ZNV3ByYmFBZz09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0=
         * }</pre>
         *
         * <p><b>⚠️ Important:</b> If your Base64-encoded string ends with {@code Cg==},
         * it indicates that the original PEM file contained a trailing newline.
         * You must remove it before encoding, otherwise Go-based TLS clients and servers (such as Microfab)
         * will fail to parse it properly.</p>
         *
         * <p><h3>Java Example</h3></p>
         * <pre>{@code
         * @Tls(
         *     enabled = true,
         *     ca = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNLRENDQWMyZ0F3" +
         *          "SUJBZ0lVVC9DY20wbitsTDZ5T3BtckxyUFhyUFhBVG1Vd0NnWUlLb1pJ" +
         *          "emowRUF3SXcKYVRFTE1Ba0dBMVVFQmhNQ1NWSXhEekFOQmdOVkJBZ01C" +
         *          "bFJsYUhKaGJqRVBNQTBHQTFVRUJ3d0dWR1ZvY21GdQpNUTh3RFFZRFZR" +
         *          "UUtEQVpHWVdKcGJtOHhEVEFMQmdOVkJBc01CRlJsYzNReEdEQVdCZ05W" +
         *          "QkFNTUQwMXBZM0p2ClptRmlJRlJNVXlCRFFUQWVGdzB5TlRFd01URXdO" +
         *          "VFF4TkRSYUZ3MHpOVEV3TURrd05UUXhORFJhTUdreEN6QUoKQmdOVkJB" +
         *          "WVRBa2xTTVE4d0RRWURWUVFJREFaVVpXaHlZVzR4RHpBTkJnTlZCQWNN" +
         *          "QmxSbGFISmhiakVQTUEwRwpBMVVFQ2d3R1JtRmlhVzV2TVEwd0N3WURW" +
         *          "UVFMREFSVVpYTjBNUmd3RmdZRFZRUUREQTlOYVdOeWIyWmhZaUJVClRG" +
         *          "TWdRMEV3V1RBVEJnY3Foa2pPUFFJQkJnZ3Foa2pPUFFNQkJ3TkNBQVJV" +
         *          "TzZ6MTFWc0txSXF0UFRodVE1V3YKNTJoK3h5eC8zTU50SVZRZmJJc0o2" +
         *          "TGJ3VFFwTm41YjdzNGNYNlNUY0hMbEFwK05obnIxeWxUWkNISGxWeUYx" +
         *          "NwpvMU13VVRBZEJnTlZIUTRFRmdRVTRKWGNNWEtSdTJqRGV2MEtqYzNM" +
         *          "RXRvK1JKTXdId1lEVlIwakJCZ3dGb0FVCjRKWGNNWEtSdTJqRGV2MEtq" +
         *          "YzNMRXRvK1JKTXdEd1lEVlIwVEFRSC9CQVV3QXdFQi96QUtCZ2dxaGtq" +
         *          "T1BRUUQKQWdOSkFEQkdBaUVBd3pYcUhDbjhPQmVOOGJ2NnNUd2k0RWxT" +
         *          "alZIamkrWVdRTlZMbmhWMVRFc0NJUUNpZWdtaApTSHp1NE95SWRZMlUv" +
         *          "eDA0Nzhob3BKZ0pFVU1iK2ZNV3ByYmFBZz09Ci0tLS0tRU5EIENFUlRJ" +
         *          "RklDQVRFLS0tLS0="
         * )
         * }</pre>
         */
        String ca() default "";
    }

    /**
     * Annotation for declaring chaincode configuration in a Microfab-based
     * test or runtime environment.
     * <p>
     * This annotation can be applied to fields or used as a meta-annotation
     * to define chaincode metadata that will later be consumed by Microfab
     * or Fabric-related utilities in your test framework.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * @Microfab(
     *      @Chaincode(
     *           name = "asset-transfer",
     *           version = "1.2",
     *           address = "127.0.0.1:7052",
     *           type = Chaincode.Type.CCAAS
     *      )
     * )
     * public class MyTest {
     * }
     * }</pre>
     *
     * <h2>Defaults</h2>
     * <ul>
     *   <li>{@link #name()} → {@code "test-cc"}</li>
     *   <li>{@link #version()} → {@code "1.0"}</li>
     *   <li>{@link #address()} → {@code "127.0.0.1:9999"}</li>
     *   <li>{@link #type()} → {@link Chaincode.Type#CCAAS}</li>
     * </ul>
     *
     * <p>
     * These defaults are suitable for quick testing scenarios in development
     * environments, but can be overridden for production-like configurations.
     *
     * @see Chaincode.Type
     */
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface Chaincode {

        /**
         * Predefined default instance of {@link Chaincode} for special scenarios
         * where no actual chaincode annotation is available or required.
         * <p>
         * This proxy-based instance behaves like a real {@link Chaincode} annotation
         * and can be used as a placeholder in:
         * <ul>
         *   <li>Testing environments where chaincode is not deployed yet</li>
         *   <li>Factory methods that expect a {@link Chaincode} annotation but no
         *       real chaincode instance exists</li>
         *   <li>Default metadata generation in tools or libraries interacting with
         *       Microfab or Fabric networks</li>
         * </ul>
         * Implementation uses a dynamic {@link java.lang.reflect.Proxy} to simulate
         * a concrete annotation instance. This allows it to be passed anywhere a
         * {@link Chaincode} annotation is expected without instantiating a real annotation.
         */
        Chaincode EMPTY = (Chaincode) Proxy.newProxyInstance(
                Chaincode.class.getClassLoader(),
                new Class[]{Chaincode.class},
                (obj, method, args) -> {
                    throw new UnsupportedOperationException("empty chaincode");
                });

        /**
         * The logical name of the chaincode.
         * <p>
         * This should match the name specified during deployment or
         * lifecycle operations. It uniquely identifies the chaincode
         * within a Fabric network.
         *
         * @return the chaincode name (default: {@code "test-cc"})
         */
        String name() default "test-cc";

        /**
         * The version of the chaincode.
         * <p>
         * Typically incremented when the chaincode implementation
         * changes. This value must align with the version declared
         * when installing/approving chaincode definitions.
         *
         * @return the chaincode version (default: {@code "1.0"})
         */
        String version() default "1.0";

        /**
         * The endpoint (<code>host:port</code>) where the chaincode server is available.
         * <p>
         * This property is critical in Chaincode-as-a-Service (CCaaS) mode, because peers
         * connect directly to the external chaincode process at this address.
         * For in-process Java chaincode, this value is typically unused.
         * </p>
         *
         * <h3>Certificate and hostname validation</h3>
         * Microfab generates TLS certificates for chaincode servers with the following
         * Subject Alternative Names (SANs):
         * <pre>
         *   *.127-0-0-1.nip.io
         *   127.0.0.1
         *   localhost
         *   0.0.0.0
         *   *.localho.st
         * </pre>
         * To pass TLS hostname verification, the configured address must match one of
         * these patterns. For example:
         * <ul>
         *   <li>{@code mycc.127-0-0-1.nip.io:9999}</li>
         *   <li>{@code localhost:9999}</li>
         *   <li>{@code mycc.localho.st:9999}</li>
         * </ul>
         *
         * <h3>Network visibility requirements</h3>
         * The peer container (running inside Microfab) must be able to reach the chaincode
         * process at the given address. This means:
         * <ul>
         *   <li>If the chaincode runs on the <b>same Docker host</b>, you may use
         *   {@code host.docker.internal} (resolves to the host from inside Docker Desktop).
         *   On Linux Docker Engine, you may need to add:
         *   <pre>{@code
         *   docker run --add-host=host.docker.internal:host-gateway ...
         *   }</pre>
         *   </li>
         *   <li>If the chaincode runs on a <b>different machine</b>, use that machine’s
         *   routable IP address or DNS name, provided it is reachable from the Microfab
         *   container.</li>
         * </ul>
         *
         * <p>
         * In summary, the configured address must both:
         * <ol>
         *   <li>Match one of the SAN entries in the Microfab TLS certificate, and</li>
         *   <li>Be network-accessible from inside the Microfab container.</li>
         * </ol>
         * </p>
         *
         * @return the chaincode service address (default:
         * {@code "localhost:9999"}).
         */
        String address() default "localhost:9999";

        /**
         * The type of chaincode being deployed or tested.
         * <ul>
         *   <li>{@link Chaincode.Type#CCAAS} → Chaincode-as-a-Service (runs in an external process).</li>
         *   <li>{@link Chaincode.Type#JAVA} → In-process Java chaincode (embedded in the peer runtime).</li>
         * </ul>
         *
         * @return the chaincode type (default: {@link Chaincode.Type#CCAAS})
         */
        Chaincode.Type type() default Chaincode.Type.CCAAS;

        /**
         * The builder class responsible for packaging, deploying,
         * and running the chaincode.
         * <p>
         * Defaults to {@link CcaasBuilder}.
         * </p>
         *
         * @return the class of the {@link CcBuilder} to use
         */
        Class<? extends CcBuilder> builder() default CcaasBuilder.class;

        /**
         * Supported chaincode implementation types.
         */
        @Getter
        enum Type {

            /**
             * Unknown type
             */
            UNKNOWN("unknown"),

            /**
             * Chaincode-as-a-Service (CCaaS).
             * <p>
             * The peer connects to a chaincode server running externally,
             * typically packaged with a {@code connection.json} descriptor.
             */
            CCAAS("ccaas"),

            /**
             * Java chaincode embedded in the peer.
             * <p>
             * The chaincode runs inside the peer JVM without requiring
             * a separate server process.
             */
            JAVA("java");

            private final String name;

            Type(String name) {
                this.name = name;
            }
        }
    }
}
