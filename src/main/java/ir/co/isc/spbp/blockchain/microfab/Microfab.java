package ir.co.isc.spbp.blockchain.microfab;

import ir.co.isc.spbp.blockchain.microfab.builder.CcBuilder;
import ir.co.isc.spbp.blockchain.microfab.builder.CcaasBuilder;
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
         * Certificate in PEM format, if TLS is enabled.
         * <p>
         * Default: empty (no certificate).
         */
        String certificate() default "";

        /**
         * Private key in PEM format, if TLS is enabled.
         * <p>
         * Default: empty (no private key).
         */
        String privateKey() default "";

        /**
         * CA certificate in PEM format, if TLS is enabled.
         * <p>
         * Default: empty (no CA).
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
