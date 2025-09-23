package ir.co.isc.spbp.blockchain.microfab;

import ir.co.isc.spbp.blockchain.microfab.model.Config;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ChaincodeProxy;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.ThrowingFunction;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

import static ir.co.isc.spbp.blockchain.microfab.utils.Util.Time.parse;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A specialized Testcontainers container for running Hyperledger
 * <a href="https://github.com/hyperledger-labs/microfab">Microfab</a>.
 * <p>
 * This container automatically applies the provided {@link Config} object
 * to the environment of the Microfab process using the {@code MICROFAB_CONFIG}
 * environment variable.
 * <p>
 * Typical usage:
 * <pre>{@code
 * Config config = Config.builder()
 *     .image("hyperledger-labs/microfab:latest")
 *     .port(8080)
 *     .directory("/home/microfab/data")
 *     .build();
 *
 * try (MicrofabContainer<?> container = new MicrofabContainer<>(config)) {
 *     container.start();
 *     // interact with Fabric gateway services
 * }
 * }</pre>
 *
 * @param <SELF> the generic type used for fluent container configuration
 */
@Getter
public class MicrofabContainer<SELF extends MicrofabContainer<SELF>> extends FixedHostPortGenericContainer<SELF> {

    private static final String MICROFAB_DAEMON_STARTUP_LOG_PATTERN = "\\[.*microfabd] .* Microfab started in \\d+ms\\n";
    private static final String KV_LEDGER_COMMIT_LOG_PATTERN = "\\[.*peer] .* \\[kvledger] commit -> \\[.*] Committed block \\[1] with 1 transaction\\(s\\) in \\d+ms.*\\n";
    private static final String GOSSIP_LOG_PATTERN = "\\[.*peer] .* \\[gossip.channel] reportMembershipChanges -> \\[\\[.*] Membership view has changed. peers went online: .* , current view: .*]\\n";

    /**
     * The configuration that was used to create this container.
     */
    private final Config config;

    /**
     * Creates a new Microfab container with the specified configuration.
     *
     * @param config the {@link Config} object containing domain, port,
     *               organizations, channels, and other network settings.
     */
    @SuppressWarnings("deprecation")
    public MicrofabContainer(@NotNull Config config) {
        super(config.getImage());
        this.config = config;
    }

    /**
     * Configures the container after construction but before startup.
     * <p>
     * This method overrides {@link FixedHostPortGenericContainer#configure()}
     * to apply the Microfab-specific setup steps.
     */
    @Override
    protected final void configure() {
        withLabel("component", "microfab");
        withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("microfab")));
        configureFixedPorts();
        configureWaitStrategy();
        configureStartupTimeout();
        configureEnvironment();
        addChaincodeHost();
//        withEnv("FABRIC_LOGGING_SPEC", "debug");
    }

    /**
     * Exposes a fixed host port mapped to the container port defined in {@link Config#getPort()}.
     */
    protected void configureFixedPorts() {
        withFixedExposedPort(config.getPort(), config.getPort());
    }

    /**
     * Configures the container's wait strategy to ensure that Microfab and the
     * Fabric network it manages are fully initialized before tests proceed.
     * <p>
     * The wait strategy combines multiple log-based checks:
     * <ul>
     *   <li><b>Microfab daemon startup:</b><br>
     *       Waits for the log entry
     *       <pre>[ microfabd] ... Microfab started in ...ms</pre>
     *       which signals that the Microfab service itself has finished starting.</li>
     *
     *   <li><b>Ledger commit readiness:</b><br>
     *       For each endorsing organization in every configured channel, waits for
     *       a commit log entry of the form:
     *       <pre>[ orgXpeer] ... INFO [kvledger] commit -> [channel] Committed block [1] ...</pre>
     *       The total number of expected commit messages is the sum of
     *       {@code channel.getEndorsingOrganizations().size()} across all channels,
     *       ensuring that each peer has committed the initial block on every channel.</li>
     *
     *   <li><b>Gossip network establishment (multi-org channels only):</b><br>
     *       For channels with more than one endorsing organization, waits for
     *       gossip-related log entries (patterned by
     *       {@link MicrofabContainer#GOSSIP_LOG_PATTERN}) to confirm that peers
     *       across organizations have established gossip communication links.
     *       The total number of expected gossip messages is the sum of
     *       {@code channel.getEndorsingOrganizations().size()} across all
     *       multi-org channels.</li>
     * </ul>
     * <p>
     * By waiting for these conditions, this strategy guarantees that:
     * <ol>
     *   <li>The Microfab daemon process is running,</li>
     *   <li>All peers have committed the genesis block on their respective channels, and</li>
     *   <li>The gossip layer is operational for channels that span multiple organizations.</li>
     * </ol>
     * This ensures the test environment is stable and ready for gateway or client interactions.
     */
    protected void configureWaitStrategy() {
        Set<Config.Channel> channels = config.getChannels();
        int gossipLogCount = channels.stream()
                .map(Config.Channel::getEndorsingOrganizations)
                .map(Set::size)
                .filter(s -> s > 1)
                .reduce(0, Integer::sum);
        int kvLedgerLogCount = channels.stream()
                .map(Config.Channel::getEndorsingOrganizations)
                .map(Set::size)
                .reduce(0, Integer::sum);
        WaitAllStrategy waits = new WaitAllStrategy()
                .withStrategy(Wait.forLogMessage(MICROFAB_DAEMON_STARTUP_LOG_PATTERN, 1))
                .withStrategy(Wait.forLogMessage(KV_LEDGER_COMMIT_LOG_PATTERN, kvLedgerLogCount));
        if (gossipLogCount > 0) {
            waits.withStrategy(Wait.forLogMessage(GOSSIP_LOG_PATTERN, gossipLogCount));
        }
        waitingFor(waits);
    }

    /**
     * Applies a startup timeout based on the {@link Config#getTimeout()} string.
     * <p>
     * Supports values like {@code "30s"}, {@code "10m"}, {@code "1h"}.
     */
    protected void configureStartupTimeout() {
        withStartupTimeout(parse(config.getTimeout()));
    }

    /**
     * Sets the {@code MICROFAB_CONFIG} environment variable inside the container,
     * serializing the {@link Config} object as pretty-printed JSON.
     */
    protected void configureEnvironment() {
        withEnv("MICROFAB_CONFIG", new JSONObject(config).toString(2));
    }

    /**
     * Configures hostname and port mappings for all chaincodes defined in the configuration.
     * <p>
     * This method delegates to {@link ChaincodeProxy#expose(MicrofabContainer, Config.Chaincode...)}
     * to set up the necessary mappings inside the Microfab container.
     * </p>
     *
     * <h3>What happens under the hood</h3>
     * <ul>
     *   <li>Each chaincode’s externally exposed port is registered with
     *       {@link org.testcontainers.containers.PortForwardingContainer PortForwardingContainer},
     *       allowing it to be accessible from the Docker network.</li>
     *   <li>A hostname mapping of the form {@code chaincodeName.localho.st → hostIp} is
     *       added via {@link org.testcontainers.containers.GenericContainer#withExtraHost(String, String)}.</li>
     * </ul>
     *
     * <h3>Why this is required</h3>
     * <ol>
     *   <li><b>Certificate validation:</b> Microfab chaincode TLS certificates
     *       use {@code *.localho.st} as a Subject Alternative Name (SAN), so
     *       the hostname must resolve correctly for the TLS handshake to succeed.</li>
     *   <li><b>Network routing:</b> Peers inside the Docker network must be able
     *       to resolve {@code chaincodeName.localho.st} to the Docker host’s IP
     *       in order to reach the chaincode process.</li>
     * </ol>
     *
     * @see ChaincodeProxy#expose(MicrofabContainer, Config.Chaincode...)
     * @see org.testcontainers.containers.PortForwardingContainer
     */
    protected void addChaincodeHost() {
        ChaincodeProxy.expose(this, config.getChaincdoes().toArray(new Config.Chaincode[0]));
    }

    /**
     * Enables Fabric peer <b>development mode</b> for chaincode.
     * <p>
     * This sets the environment variable:
     * <pre>
     * CORE_PEER_CHAINCODE_MODE=dev
     * </pre>
     * so that the peer will not launch chaincode containers internally.
     * Instead, you can manually run a chaincode process (e.g.,
     * {@code ContractRouter.main()} in CCAAS mode) on your host,
     * pointing it back to the peer.
     * <p>
     * This gives you full control over debugging and managing
     * chaincode processes outside the container.
     *
     * @return this container instance for method chaining
     */
    public SELF withChaincodeDevMode() {
        withEnv("CORE_CHAINCODE_MODE", "dev");
        return self();
    }

    /**
     * Extracts the <code>state.json</code> file from the running Microfab container
     * and returns its contents as a UTF-8 string.
     *
     * <p>
     * The <code>state.json</code> file is generated by Microfab during startup and
     * contains a snapshot of the internal network state, including identities,
     * certificate authorities, TLS materials, and other configuration details.
     * This method copies the file from inside the container at the location
     * <code>{@link Config#getDirectory()}/state.json</code> and reads its full
     * contents into memory.
     * </p>
     *
     * <h2>Usage</h2>
     * <pre>{@code
     * String stateJson = microfabContainer.getState();
     * State state = new ObjectMapper().readValue(stateJson, State.class);
     * }</pre>
     *
     * <p>
     * This method is typically used in integration tests or automation workflows
     * where access to Microfab’s generated cryptographic material and configuration
     * is required on the host side (e.g., parsing identities for SDK clients).
     * </p>
     *
     * @return the raw JSON contents of <code>state.json</code> as a string
     */
    public String getState() {
        return copyFileFromContainer(
                config.getDirectory() + "/state.json",
                in -> {
                    try (in) {
                        return new String(in.readAllBytes(), UTF_8);
                    }
                });
    }

    /**
     * Computes the SHA-256 hash of a packaged chaincode archive
     * (<code>.tar.gz</code>) inside the running Microfab container.
     * <p>
     * The chaincode package is expected at:
     * <pre>
     * {config.directory}/chaincodes/{chaincode}/{chaincode}.tar.gz
     * </pre>
     * where:
     * <ul>
     *   <li>{@code config.directory} is the base directory configured for Microfab, and</li>
     *   <li>{@code chaincode} is the name of the chaincode supplied to this method.</li>
     * </ul>
     * <p>
     * Internally, this method copies the target file from the container using
     * {@link org.testcontainers.containers.GenericContainer#copyFileFromContainer(String, ThrowingFunction)} and computes
     * its SHA-256 digest via {@link java.security.MessageDigest}.
     * The resulting hash is returned as a lowercase hexadecimal string.
     * <p>
     * <b>Background:</b><br>
     * In the Hyperledger Fabric chaincode lifecycle, each installed chaincode
     * is uniquely identified by its <em>package ID</em>, which is formed from
     * the SHA-256 hash of the <code>.tar.gz</code> archive and its label.
     * This method allows you to reproduce that hash calculation directly
     * from a Microfab-managed container, making it possible to:
     * <ul>
     *   <li>Verify package integrity,</li>
     *   <li>Programmatically derive the expected <code>packageID</code>, and</li>
     *   <li>Troubleshoot packaging or deployment inconsistencies.</li>
     * </ul>
     *
     * @param chaincode the instance of the {@link Config.Chaincode}; used to locate the
     *                  corresponding <code>.tar.gz</code> package inside the container.
     * @return the SHA-256 hash of the chaincode package, formatted as a
     * lowercase hexadecimal string.
     * @throws RuntimeException if reading the package file or computing the digest fails.
     */
    public String getHash(Config.Chaincode chaincode) {
        return copyFileFromContainer(
                "%s/%s/%s.tar.gz".formatted(
                        config.getDirectory(),
                        chaincode.getDir(),
                        chaincode.getName()),
                pkg -> {
                    try (pkg) {
                        return HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(pkg.readAllBytes()));
                    }
                });
    }

    /**
     * Creates a text file inside the running Microfab container.
     * <p>
     * The specified string content is converted to UTF-8 encoded bytes
     * and written to a file at the given destination path within the container.
     * This is a convenience method for transferring small text files,
     * such as configuration files or certificates, into the container.
     * </p>
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * microfabContainer.createFile("sample-config", "/var/hyperledger/config.txt");
     * }</pre>
     *
     * @param content     the textual content to be written into the file (non-null).
     * @param destination the absolute path inside the container where the file will be created.
     * @throws IllegalStateException if the container is not running.
     */
    public void createFile(String content, String destination) {
        copyFileToContainer(Transferable.of(content.getBytes(UTF_8)), destination);
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            ChaincodeProxy.terminate();
        }
    }
}
