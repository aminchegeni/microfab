package ir.co.isc.spbp.blockchain.microfab.model;

import ir.co.isc.spbp.blockchain.microfab.Microfab;
import ir.co.isc.spbp.blockchain.microfab.builder.CcBuilder;
import ir.co.isc.spbp.blockchain.microfab.builder.CcaasBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.json.JSONPropertyIgnore;
import org.json.JSONPropertyName;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ir.co.isc.spbp.blockchain.microfab.Microfab.Chaincode.Type.CCAAS;
import static ir.co.isc.spbp.blockchain.microfab.utils.Util.toUnchecked;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

/**
 * Represents the configuration for a Microfab instance.
 * <p>
 * A {@code Config} defines the domain, organizations, channels,
 * and infrastructure options for a local Fabric network managed
 * by Microfab. The configuration closely mirrors the JSON structure
 * expected by {@code microfabd}.
 * <p>
 * See:
 * <a href="https://github.com/hyperledger-labs/microfab">Hyperledger Labs - Microfab</a>
 */
@Data
@Builder(builderClassName = "Builder")
public class Config {

    /**
     * The Docker image used to run Microfab.
     * <p>
     * Default: {@code "hyperledger-labs/microfab:latest"}
     */
    @Getter(onMethod_ = @JSONPropertyIgnore)
    @lombok.Builder.Default
    private String image = "hyperledger-labs/microfab:latest";
    /**
     * The domain used for generating hostnames of peers, orderers,
     * and certificate authorities.
     * <p>
     * For example: {@code 127-0-0-1.nip.io}
     */
    @lombok.Builder.Default
    private String domain = "127-0-0-1.nip.io";
    /**
     * The port used to expose the Microfab REST API.
     * <p>
     * Default: {@code 8080}
     */
    @lombok.Builder.Default
    private int port = 8080;
    /**
     * The filesystem directory where Microfab will persist state,
     * such as ledgers and MSP material.
     * <p>
     * Example: {@code /home/microfab/data}
     */
    @lombok.Builder.Default
    private String directory = "/home/microfab/data";
    /**
     * The single ordering organization in the network.
     * <p>
     * Microfab currently supports one orderer organization only.
     */
    @Getter(onMethod_ = @JSONPropertyName("ordering_organization"))
    @lombok.Builder.Default
    private Organization orderingOrganization = Organization.builder().name("Orderer").build();
    /**
     * The endorsing organizations that will host peers in the network.
     * <p>
     * Each organization contributes at least one peer to the network.
     */
    @Getter(onMethod_ = @JSONPropertyName("endorsing_organizations"))
    @lombok.Builder.Default
    private Set<Organization> endorsingOrganizations = Set.of(Organization.builder().name("Org1").build());
    /**
     * The set of channels to be created when the network is bootstrapped.
     * <p>
     * Each channel defines its name, member organizations, and optional
     * capability level.
     */
    @lombok.Builder.Default
    private Set<Channel> channels = Set.of(Channel.builder()
            .name("channel1")
            .endorsingOrganizations(Set.of("Org1"))
            .build());
    /**
     * The global Fabric capability level to apply to the network and channels.
     * <p>
     * This acts as a default, but may be overridden per channel.
     * <p>
     * Examples: {@code V2_0}, {@code V2_5}
     */
    @Getter(onMethod_ = @JSONPropertyName("capability_level"))
    @lombok.Builder.Default
    private String capabilityLevel = "V2_5";
    /**
     * Whether CouchDB should be deployed alongside peers as the state database.
     * <p>
     * Default: {@code true}
     */
    @lombok.Builder.Default
    private boolean couchdb = true;
    /**
     * Whether Fabric Certificate Authorities (CAs) should be deployed
     * for each organization.
     * <p>
     * Default: {@code true}
     */
    @Getter(onMethod_ = @JSONPropertyName("certificate_authorities"))
    @lombok.Builder.Default
    private boolean certificateAuthorities = true;
    /**
     * Timeout string used by Microfab for network operations,
     * expressed as a Go-style duration string.
     * <p>
     * Example: {@code 30s}, {@code 1m}, {@code 2m30s}
     */
    @lombok.Builder.Default
    private String timeout = "30s";
    /**
     * TLS configuration controlling whether TLS is enabled
     * and the certificate material used.
     */
    @lombok.Builder.Default
    private Tls tls = Tls.builder().build();
    /**
     * The set of chaincodes to be used when the network is bootstrapped.
     * <p>
     * Each chaincode defines its name, version, address and type.
     */
    @Getter(onMethod_ = @JSONPropertyIgnore)
    @lombok.Builder.Default
    private Set<Chaincode> chaincdoes = Collections.emptySet();

    // ------------------------------------------------------------------------
    // factory method
    // ------------------------------------------------------------------------

    /**
     * Creates a {@link Config} object from a {@link Microfab} microfab instance.
     *
     * @param microfab the {@link Microfab} microfab
     * @return a fully built {@link Config} instance
     */
    public static Config of(Microfab microfab) {
        return builder()
                .image(microfab.image())
                .domain(microfab.domain())
                .port(microfab.port())
                .directory(microfab.directory())
                .orderingOrganization(Organization.builder()
                        .name(microfab.orderingOrganization().name())
                        .build())
                .endorsingOrganizations(Arrays.stream(microfab.endorsingOrganizations())
                        .map(Microfab.Organization::name)
                        .map(name -> Organization.builder().name(name).build())
                        .collect(Collectors.toSet()))
                .channels(Arrays.stream(microfab.channels())
                        .map(Channel::of)
                        .collect(Collectors.toSet()))
                .capabilityLevel(microfab.capabilityLevel())
                .couchdb(microfab.couchdb())
                .certificateAuthorities(microfab.certificateAuthorities())
                .timeout(microfab.timeout())
                .tls(Tls.of(microfab.tls()))
                .chaincdoes(Arrays.stream(microfab.chaincodes())
                        .map(Chaincode::of)
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Returns the absolute path to the Orderer's TLS CA certificate file.
     * <p>
     * This file is expected to be located at: <pre>{@code <directory>/orderer/tls/ca.pem}</pre>
     *
     * @return the full path to the Orderer's TLS CA certificate.
     */
    @JSONPropertyIgnore
    public String getOrdererTlsCaPem() {
        return directory + "/orderer/tls/ca.pem";
    }

    /**
     * Constructs the API URL for the peer service of the given organization.
     * <p>
     * The returned URL follows the format:
     * <pre>{@code <org>peer-api.<domain>:<port>}</pre>
     * Where {@code <org>} is the lowercase organization name, {@code <domain>} is the configured domain
     * and {@code port} is the configured port number.
     *
     * @param org the name of the organization (non-null).
     * @return the constructed peer API URL.
     * @throws NullPointerException if {@code org} is {@code null}.
     */
    @JSONPropertyIgnore
    public String getApiUrl(Organization org) {
        return "%s-api.%s:%d".formatted(org.getPeerId(), domain, port);
    }

    // ------------------------------------------------------------------------
    // Nested classes
    // ------------------------------------------------------------------------

    /**
     * Represents an organization in the Microfab configuration.
     * <p>
     * An organization may be an ordering organization (hosting an orderer)
     * or an endorsing organization (hosting peers).
     */
    @Data
    @lombok.Builder(builderClassName = "Builder")
    public static class Organization {

        private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]+");

        /**
         * The name of the organization.
         * <p>
         * Example: {@code Org1}, {@code Org2}, {@code Orderer}
         */
        private String name;

        /**
         * Returns the local MSP ID for a given organization.
         * <p>
         * The MSP ID is constructed by removing all non-alphanumeric characters from the provided
         * organization name and appending the suffix {@code MSP}.
         * <br>
         * For example, {@code "Org@1!"} becomes {@code "Org1MSP"}.
         *
         * @return the local MSP ID for the given organization.
         */
        @JSONPropertyIgnore
        public String getLocalMspId() {
            return "%sMSP".formatted(NON_ALPHANUMERIC.matcher(name).replaceAll(""));
        }

        /**
         * Returns the ID of the admin identity for this organization.
         * <p>
         * The admin ID is the lowercase organization name with the suffix {@code admin}.
         * <br>
         * Example: for {@code Org1}, the admin ID is {@code org1admin}.
         *
         * @return the admin identity ID.
         */
        @JSONPropertyIgnore
        public String getAdminId() {
            return "%sadmin".formatted(name.toLowerCase());
        }

        /**
         * Returns the MSP config path for the admin user of the specified organization.
         * <p>
         * The returned path follows the format:
         * <pre>{@code admin-<org>}</pre>
         * Where {@code <org>} is the lowercase version of the organization name.
         *
         * @return the admin MSP config path.
         */
        @JSONPropertyIgnore
        public String getAdminMspDir() {
            return "admin-%s".formatted(name.toLowerCase());
        }

        /**
         * Returns the ID of the CA admin identity for this organization.
         * <p>
         * The CA admin ID is the lowercase organization name with the suffix {@code caadmin}.
         * <br>
         * Example: for {@code Org1}, the CA admin ID is {@code org1caadmin}.
         *
         * @return the CA admin identity ID.
         */
        @JSONPropertyIgnore
        public String getCaAdminId() {
            return "%scaadmin".formatted(name.toLowerCase());
        }

        /**
         * Returns the peer identity ID for this organization.
         * <p>
         * The peer ID is the lowercase organization name with the suffix {@code peer}.
         * <br>
         * Example: for {@code Org1}, the peer ID is {@code org1peer}.
         *
         * @return the peer identity ID.
         */
        @JSONPropertyIgnore
        public String getPeerId() {
            return "%speer".formatted(name.toLowerCase());
        }

        /**
         * Returns the path to the configuration directory for the peer belonging to the specified organization.
         * <p>
         * The returned path follows the format:
         * <pre>{@code peer-<org>/config}</pre>
         * Where {@code <org>} is the lowercase version of the organization name.
         *
         * @return the full config path for the peer.
         */
        @JSONPropertyIgnore
        public String getPeerCfgDir() {
            return "peer-%s/config".formatted(name.toLowerCase());
        }

        /**
         * Returns the fixed ID of the ordering service node.
         * <p>
         * Microfab always uses {@code orderer} as the identifier for the ordering service.
         *
         * @return the orderer ID.
         */
        @JSONPropertyIgnore
        public String getOrdererId() {
            return "orderer";
        }

        /**
         * Returns the CA identity ID for this organization.
         * <p>
         * The CA ID is the lowercase organization name with the suffix {@code ca}.
         * <br>
         * Example: for {@code Org1}, the CA ID is {@code org1ca}.
         *
         * @return the CA identity ID.
         */
        @JSONPropertyIgnore
        public String getCaId() {
            return "%sca".formatted(name.toLowerCase());
        }

        /**
         * Returns the gateway identity ID for this organization.
         * <p>
         * The gateway ID is the lowercase organization name with the suffix {@code gateway}.
         * <br>
         * Example: for {@code Org1}, the gateway ID is {@code org1gateway}.
         *
         * @return the gateway identity ID.
         */
        @JSONPropertyIgnore
        public String getGatewayId() {
            return "%sgateway".formatted(name.toLowerCase());
        }
    }

    /**
     * Represents a Fabric channel in the Microfab configuration.
     * <p>
     * Each channel specifies its name, the endorsing organizations
     * that participate, and an optional capability level.
     */
    @Data
    @lombok.Builder(builderClassName = "Builder")
    public static class Channel {

        /**
         * The name of the channel.
         * <p>
         * Example: {@code mychannel}, {@code tokenization}
         */
        private String name;

        /**
         * The names of the organizations that are members of the channel
         * and can endorse transactions.
         * <p>
         * Example: {@code ["Org1", "Org2"]}
         */
        @Getter(onMethod_ = @JSONPropertyName("endorsing_organizations"))
        private Set<String> endorsingOrganizations;

        /**
         * Optional capability level for the channel. If not specified,
         * the global {@link Config#capabilityLevel} is used.
         * <p>
         * Example: {@code V2_0}
         */
        @Getter(onMethod_ = @JSONPropertyName("capability_level"))
        private String capabilityLevel;

        /**
         * The names of the chaincodes that and must be installed on channel.
         * <p>
         * Example: {@code ["cc1", "cc2"]}
         */
        @Getter(onMethod_ = @JSONPropertyIgnore)
        private Set<String> chaincodes;

        // ------------------------------------------------------------------------
        // factory method
        // ------------------------------------------------------------------------

        /**
         * Factory method to create a {@link Channel}
         * instance from a {@link ir.co.isc.spbp.blockchain.microfab.Microfab.Channel} annotation.
         *
         * @param channel the {@link ir.co.isc.spbp.blockchain.microfab.Microfab.Channel} annotation
         *                whose values should populate this object
         * @return a new {@link Channel} instance containing
         * the same configuration as the annotation
         */
        public static Channel of(Microfab.Channel channel) {
            String capabilityLevel = channel.capabilityLevel();
            return Channel.builder()
                    .name(channel.name())
                    .endorsingOrganizations(Set.of(channel.endorsingOrganizations()))
                    .capabilityLevel(capabilityLevel.isBlank() ? null : capabilityLevel)
                    .chaincodes(Set.of(channel.chaincodes()))
                    .build();
        }
    }

    /**
     * Represents the TLS configuration for the Microfab network.
     * <p>
     * TLS may be enabled or disabled, and optionally custom
     * certificate material can be provided.
     */
    @Data
    @lombok.Builder(builderClassName = "Builder")
    public static class Tls {

        /**
         * Whether TLS is enabled for the network.
         * <p>
         * Default: {@code false}
         */
        @lombok.Builder.Default
        private boolean enabled = false;

        /**
         * Base64 PEM-encoded certificate for TLS, if provided.
         * <p>
         * May be {@code null} to let Microfab generate one.
         */
        private String certificate;

        /**
         * Base64 PEM-encoded private key for TLS, if provided.
         * <p>
         * May be {@code null} to let Microfab generate one.
         */
        private String privateKey;

        /**
         * Base64 PEM-encoded CA certificate for TLS, if provided.
         * <p>
         * May be {@code null} to let Microfab generate one.
         */
        private String ca;

        // ------------------------------------------------------------------------
        // factory method
        // ------------------------------------------------------------------------

        /**
         * Factory method to create a {@link Tls}
         * instance from a {@link ir.co.isc.spbp.blockchain.microfab.Microfab.Tls} annotation.
         *
         * @param tls the {@link ir.co.isc.spbp.blockchain.microfab.Microfab.Tls} annotation
         *            whose values should populate this object
         * @return a new {@link Tls} instance containing
         * the same configuration as the annotation
         */
        public static Tls of(Microfab.Tls tls) {
            String certificate = tls.certificate();
            String privateKey = tls.privateKey();
            String ca = tls.ca();
            return Tls.builder()
                    .enabled(tls.enabled())
                    .certificate(certificate.isBlank() ? null : certificate)
                    .privateKey(privateKey.isBlank() ? null : privateKey)
                    .ca(ca.isBlank() ? null : ca)
                    .build();
        }

        /**
         * Returns the TLS certificate in PEM format.
         * <p>
         * The underlying {@code certificate} value is stored as a Base64-encoded string.
         * This method decodes it into its textual PEM representation.
         * </p>
         *
         * @return the decoded PEM string for the TLS certificate.
         * @throws IllegalArgumentException if the stored value is not valid Base64.
         */
        @JSONPropertyIgnore
        public String getCertificatePem() {
            return getPem(certificate);
        }

        /**
         * Returns the TLS private key in PEM format.
         * <p>
         * The underlying {@code privateKey} value is stored as a Base64-encoded string.
         * This method decodes it into its textual PEM representation.
         * </p>
         *
         * @return the decoded PEM string for the TLS private key.
         * @throws IllegalArgumentException if the stored value is not valid Base64.
         */
        @JSONPropertyIgnore
        public String getPrivateKeyPem() {
            return getPem(privateKey);
        }

        /**
         * Returns the TLS certificate authority (CA) certificate in PEM format.
         * <p>
         * The underlying {@code ca} value is stored as a Base64-encoded string.
         * This method decodes it into its textual PEM representation.
         * </p>
         *
         * @return the decoded PEM string for the CA certificate.
         * @throws IllegalArgumentException if the stored value is not valid Base64.
         */
        @JSONPropertyIgnore
        public String getCaPem() {
            return getPem(ca);
        }

        /**
         * Decodes a Base64-encoded string into its UTF-8 PEM representation.
         *
         * @param base64 the Base64-encoded value (non-null).
         * @return the decoded PEM string.
         * @throws IllegalArgumentException if {@code base64} is not valid Base64.
         */
        private String getPem(String base64) {
            return nonNull(base64) ? new String(Base64.getDecoder().decode(base64), UTF_8) : null;
        }
    }

    /**
     * Represents the configuration of a chaincode instance.
     * <p>
     * This class is a runtime-friendly equivalent of the
     * {@link ir.co.isc.spbp.blockchain.microfab.Microfab.Chaincode} annotation,
     * allowing the annotation’s properties to be captured in a
     * regular Java object for further use.
     * <p>
     * It is particularly useful when chaincode configuration
     * needs to be passed around, serialized, or manipulated
     * outside the static annotation context.
     */
    @Data
    @lombok.Builder(builderClassName = "Builder")
    public static class Chaincode {

        /**
         * The logical name of the chaincode.
         * <p>
         * This value corresponds to {@link Microfab.Chaincode#name()}.
         * It uniquely identifies the chaincode and is used
         * during packaging and installation.
         */
        @lombok.Builder.Default
        private String name = "test-cc";

        /**
         * The version of the chaincode.
         * <p>
         * This value corresponds to {@link Microfab.Chaincode#version()}.
         * It is combined with the name to produce the label
         * (e.g., {@code mycc_1.0}).
         */
        @lombok.Builder.Default
        private String version = "1.0";

        /**
         * The endpoint (<code>host:port</code>) where the chaincode
         * server is available.
         * <p>
         * This value corresponds to {@link Microfab.Chaincode#address()}.
         * <br>
         * In CCaaS scenarios, this address points to the external
         * chaincode process. For in-process Java chaincode, this
         * may be unused.
         */
        @lombok.Builder.Default
        private String address = "127.0.0.1:9999";

        /**
         * The type of the chaincode implementation.
         * <p>
         * This value corresponds to {@link Microfab.Chaincode#type()} and
         * determines whether the chaincode runs as:
         * <ul>
         *   <li>{@link Microfab.Chaincode.Type#CCAAS} - Chaincode-as-a-Service</li>
         *   <li>{@link Microfab.Chaincode.Type#JAVA} - In-process Java chaincode</li>
         * </ul>
         */
        @lombok.Builder.Default
        private Microfab.Chaincode.Type type = CCAAS;

        /**
         * The builder instance responsible for managing this chaincode’s lifecycle.
         * <p>
         * This field is initialized by default with a new {@link CcaasBuilder},
         * which provides support for Chaincode-as-a-Service (CCAAS). The builder
         * encapsulates the logic required to:
         * <ul>
         *   <li>Detect whether the chaincode is supported ({@link CcBuilder#detect}).</li>
         *   <li>Build the chaincode inside a {@link ir.co.isc.spbp.blockchain.microfab.MicrofabContainer}
         *       ({@link CcBuilder#build}).</li>
         *   <li>Run the chaincode as a service ({@link CcBuilder#run}).</li>
         * </ul>
         *
         * <p>
         * Custom {@link CcBuilder} implementations may be supplied
         * through the {@link Microfab.Chaincode#builder()} property,
         * in which case this field will be overridden with an instance of the specified class.
         * </p>
         */
        @Getter(onMethod_ = @JSONPropertyIgnore)
        @lombok.Builder.Default
        private CcBuilder builder = new CcaasBuilder();

        /**
         * The SHA-256 package hash calculated during
         * {@code peer lifecycle chaincode install}.
         * <p>
         * This value represents the deterministic digest of the
         * {@code .tar.gz} chaincode package. It is required when referencing
         * chaincode in Chaincode-as-a-Service (CCaaS) mode, since peers must be
         * given the exact {@code packageId} (label + hash) for successful
         * chaincode definition approval and commitment.
         * <p>
         * <b>Example packageId:</b><br>
         * <code>
         * mycc_1:1a2b3c4d...ff
         * </code>
         */
        @Getter(onMethod_ = @JSONPropertyIgnore)
        private String hash;

        // ------------------------------------------------------------------------
        // factory method
        // ------------------------------------------------------------------------

        /**
         * Factory method to create a {@link Chaincode}
         * instance from a {@link Microfab.Chaincode} annotation.
         *
         * @param chaincode the {@link Microfab.Chaincode} annotation
         *                  whose values should populate this object
         * @return a new {@link Chaincode} instance containing
         * the same configuration as the annotation
         */
        public static Chaincode of(Microfab.Chaincode chaincode) {
            return toUnchecked(
                    () -> Chaincode.builder()
                            .name(chaincode.name())
                            .version(chaincode.version())
                            .address(chaincode.address())
                            .type(chaincode.type())
                            .builder(chaincode.builder().getDeclaredConstructor().newInstance())
                            .build());
        }

        /**
         * Constructs the full **package identifier** for this chaincode,
         * following the convention used by the Fabric peer lifecycle.
         * <p>
         * The package identifier is normally required when approving or committing
         * a chaincode definition, and consists of:
         * <ul>
         *   <li><b>Name and version:</b> Combined as {@code name_version}.</li>
         *   <li><b>Optional content hash:</b> If the {@code hash} field is present
         *       and non-blank, it is appended after a colon. This represents the
         *       SHA-256 hash of the packaged chaincode tar.gz file as calculated by
         *       {@code peer lifecycle chaincode install}.</li>
         * </ul>
         * <p>
         * Examples:
         * <pre>
         *   // without hash
         *   "mycc_1.0"
         *
         *   // with hash
         *   "mycc_1.0:5be3266...9d1c3f4"
         * </pre>
         *
         * @return the chaincode package identifier in the format
         * {@code name_version[:hash]}
         */
        @JSONPropertyIgnore
        public String getPkgId() {
            return name + "_" + version + (nonNull(hash) && !hash.isBlank() ? ":%s".formatted(hash) : "");
        }

        /**
         * Extracts the hostname part of the configured chaincode endpoint.
         * <p>
         * The {@link #address} field is expected to be in the format
         * {@code host:port}. This method returns the substring before the colon.
         * </p>
         *
         * <p><b>Example:</b></p>
         * <pre>{@code
         *   address = "127.0.0.1:9999"
         *   getHost() → "127.0.0.1"
         * }</pre>
         *
         * @return the hostname portion of the {@link #address}.
         * @throws ArrayIndexOutOfBoundsException if {@link #address} does not contain a colon.
         */
        @JSONPropertyIgnore
        public String getHost() {
            return address.split(":")[0];
        }

        /**
         * Extracts the port number part of the configured chaincode endpoint.
         * <p>
         * The {@link #address} field is expected to be in the format
         * {@code host:port}. This method returns the numeric value after the colon.
         * </p>
         *
         * <p><b>Example:</b></p>
         * <pre>{@code
         *   address = "127.0.0.1:9999"
         *   getPort() → 9999
         * }</pre>
         *
         * @return the port portion of the {@link #address}, as an integer.
         * @throws NumberFormatException          if the port part is not a valid integer.
         * @throws ArrayIndexOutOfBoundsException if {@link #address} does not contain a colon.
         */
        @JSONPropertyIgnore
        public int getPort() {
            return Integer.parseInt(address.split(":")[1]);
        }

        public String getDir() {
            return "chaincodes/%s".formatted(name.toLowerCase());
        }
    }
}
