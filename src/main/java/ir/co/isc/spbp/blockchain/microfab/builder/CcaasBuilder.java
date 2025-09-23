package ir.co.isc.spbp.blockchain.microfab.builder;

import ir.co.isc.spbp.blockchain.microfab.Microfab;
import ir.co.isc.spbp.blockchain.microfab.MicrofabContainer;
import ir.co.isc.spbp.blockchain.microfab.NonBlockingChaincodeServer;
import ir.co.isc.spbp.blockchain.microfab.model.Config;
import ir.co.isc.spbp.blockchain.microfab.model.Connection;
import ir.co.isc.spbp.blockchain.microfab.model.Metadata;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.contract.ContractRouter;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.containers.GenericContainer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.stefanbirkner.systemlambda.SystemLambda.WithEnvironmentVariables;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static ir.co.isc.spbp.blockchain.microfab.model.Config.Chaincode;
import static ir.co.isc.spbp.blockchain.microfab.model.Config.Channel;
import static ir.co.isc.spbp.blockchain.microfab.utils.Util.toUnchecked;
import static java.nio.file.Files.*;

/**
 * Implementation of {@link CcBuilder} for Chaincode-as-a-Service (CCaaS).
 * <p>
 * This builder automates the complete lifecycle of deploying and running
 * external chaincodes (CCaaS) inside a {@link MicrofabContainer}.
 * It handles packaging, installation, approval, commit readiness checks,
 * and commit operations, as well as starting the chaincode server process.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li><b>Packaging:</b> Generates {@code metadata.json} and {@code connection.json}
 *   for the chaincode, archives them into a TAR.GZ, and computes a package hash.</li>
 *   <li><b>Installation:</b> Installs the chaincode package on endorsing peers
 *   that belong to the channel’s endorsing organizations.</li>
 *   <li><b>Approval:</b> Approves the chaincode definition for each endorsing org,
 *   including TLS support if enabled.</li>
 *   <li><b>Commit Readiness:</b> Verifies whether the chaincode definition
 *   has sufficient approvals to be committed.</li>
 *   <li><b>Commit:</b> Commits the chaincode definition to the channel,
 *   making it available for use.</li>
 *   <li><b>Runtime:</b> Launches a {@link NonBlockingChaincodeServer} backed
 *   by a {@link ContractRouter} in a non-blocking fashion. TLS is configured
 *   dynamically using temporary files when enabled.</li>
 * </ol>
 *
 * <h3>Why this is needed</h3>
 * <p>
 * Hyperledger Fabric’s external builder model requires additional configuration
 * and lifecycle steps for CCaaS chaincodes. This builder encapsulates these
 * steps, so test environments using Microfab can transparently support
 * external chaincode execution.
 * </p>
 *
 * <h3>Key Integration Points</h3>
 * <ul>
 *   <li>Relies on {@link Config} to determine endorsing organizations,
 *   peer addresses, and TLS configuration.</li>
 *   <li>Uses Testcontainers APIs ({@link GenericContainer}, {@link ExecConfig})
 *   to run Fabric CLI commands inside the Microfab container.</li>
 *   <li>Uses {@code system-lambda} to inject environment variables when
 *   starting the chaincode process in the same JVM.</li>
 * </ul>
 *
 * <p>
 * This builder is specific to {@link Microfab.Chaincode.Type#CCAAS}.
 * Other chaincode models can be supported by implementing additional
 * {@link CcBuilder} strategies.
 * </p>
 */
@Slf4j
public class CcaasBuilder implements CcBuilder {

    private static void chown(Config config, GenericContainer<?> c) {
        String workDir = config.getDirectory();
        String[] command = new String[]{"chown", "-R", "microfab:microfab", "chaincodes"};
        Container.ExecResult result = toUnchecked(
                () -> c.execInContainer(
                        ExecConfig.builder()
                                .user("root")
                                .workDir(workDir)
                                .command(command)
                                .build())
        );
        log.info(Arrays.toString(command));
        log.info(result.toString());
    }

    private static void targz(Config config, Chaincode chaincode, GenericContainer<?> c) {
        String baseDir = config.getDirectory();
        String workDir = baseDir + "/" + chaincode.getDir();
        String[] command1 = new String[]{"tar", "-czf", "code.tar.gz", "connection.json"};
        Container.ExecResult step1 = toUnchecked(
                () -> c.execInContainer(
                        ExecConfig.builder()
                                .user("microfab")
                                .workDir(workDir)
                                .command(command1)
                                .build())
        );
        log.info(Arrays.toString(command1));
        log.info(step1.toString());

        String[] command2 = new String[]{"tar", "-czf", "%s.tar.gz".formatted(chaincode.getName()), "metadata.json", "code.tar.gz"};
        Container.ExecResult step2 = toUnchecked(
                () -> c.execInContainer(
                        ExecConfig.builder()
                                .user("microfab")
                                .workDir(workDir)
                                .command(command2)
                                .build())
        );
        log.info(Arrays.toString(command2));
        log.info(step2.toString());
    }

    private static void _package(MicrofabContainer<?> container, Chaincode chaincode) {
        Config config = container.getConfig();
        String baseDir = config.getDirectory();
        Metadata metadata = Metadata.of(chaincode);
        Connection connection = Connection.of(config, chaincode);
        // override connection address to using chaincode proxy and make host tls friendly
        connection.setAddress("%s.localho.st:%d".formatted(chaincode.getName(), chaincode.getPort()));
        container.createFile(metadata.toJson(), baseDir + "/" + chaincode.getDir() + "/metadata.json");
        container.createFile(connection.toJson(), baseDir + "/" + chaincode.getDir() + "/connection.json");
        chown(config, container);
        targz(config, chaincode, container);
        String hash = container.getHash(chaincode);
        chaincode.setHash(hash);
    }

    private static void install(MicrofabContainer<?> container, Channel channel, Chaincode chaincode) {
        // FABRIC_LOGGING_SPEC=debug
        // FABRIC_CFG_PATH=/home/microfab/data/peer-mat/config
        // CORE_PEER_ADDRESS=matpeer-api.127-0-0-1.nip.io:8080
        // CORE_PEER_MSPCONFIGPATH=/home/microfab/data/admin-mat
        // peer lifecycle chaincode install hello.tar.gz
        String ccDir = "%s/%s.tar.gz".formatted(chaincode.getDir(), chaincode.getName());
        String[] command = new String[]{"peer", "lifecycle", "chaincode", "install", ccDir};
        runLifecycle(container, channel, command, true);
    }

    private static void approveForMyOrg(MicrofabContainer<?> container, Channel channel, Chaincode chaincode) {
        // FABRIC_LOGGING_SPEC=debug
        // FABRIC_CFG_PATH=/home/microfab/data/peer-mat/config
        // CORE_PEER_ADDRESS=matpeer-api.127-0-0-1.nip.io:8080
        // CORE_PEER_MSPCONFIGPATH=/home/microfab/data/admin-mat
        // peer lifecycle chaincode approveformyorg
        // --channelID tokenization
        // --name hello
        // --version 1.0
        // --package-id hello_1.0
        // --sequence 1
        // --tls
        // --cafile /home/microfab/data/orderer/tls/ca.pem
        Config config = container.getConfig();
        boolean isTlsEnabled = config.getTls().isEnabled();
        String[] command = new String[]{
                "peer", "lifecycle", "chaincode", "approveformyorg",
                "--channelID", channel.getName(),
                "--name", chaincode.getName(),
                "--version", chaincode.getVersion(),
                "--package-id", chaincode.getPkgId(),
                "--sequence", "1",
                "--cafile", config.getOrdererTlsCaPem(),
                isTlsEnabled ? "--tls" : ""
        };
        runLifecycle(container, channel, command, true);
    }

    public static void checkCommitReadiness(MicrofabContainer<?> container, Channel channel, Chaincode chaincode) {
        // FABRIC_LOGGING_SPEC=debug
        // FABRIC_CFG_PATH=/home/microfab/data/peer-mat/config
        // CORE_PEER_ADDRESS=matpeer-api.127-0-0-1.nip.io:8080
        // peer lifecycle chaincode checkcommitreadiness
        // --channelID tokenization
        // --name hello
        // --version 1.0
        // --sequence 1
        // --output json
        String[] command = new String[]{
                "peer", "lifecycle", "chaincode", "checkcommitreadiness",
                "--channelID", channel.getName(),
                "--name", chaincode.getName(),
                "--version", chaincode.getVersion(),
                "--sequence", "1",
                "--output", "json"
        };
        runLifecycle(container, channel, command, false);
    }

    private static void commit(MicrofabContainer<?> container, Channel channel, Chaincode chaincode) {
        Config config = container.getConfig();
        Set<Config.Organization> validOrganizations = config.getEndorsingOrganizations();
        // FABRIC_LOGGING_SPEC=debug
        // FABRIC_CFG_PATH=/home/microfab/data/peer-mat/config
        // CORE_PEER_ADDRESS=matpeer-api.127-0-0-1.nip.io:8080
        // peer lifecycle chaincode commit
        // --channelID tokenization
        // --name hello
        // --version 1.0
        // --sequence 1
        // --cafile /home/microfab/data/orderer/tls/ca.pem
        // --tls
        Set<String> channelOrganizations = channel.getEndorsingOrganizations();
        validOrganizations.stream()
                .filter(o -> channelOrganizations.contains(o.getName()))
                .findFirst()
                .ifPresent(org -> {
                    String workDir = config.getDirectory();
                    boolean isTlsEnabled = config.getTls().isEnabled();
                    String[] command = new String[]{
                            "peer", "lifecycle", "chaincode", "commit",
                            "--channelID", channel.getName(),
                            "--name", chaincode.getName(),
                            "--version", chaincode.getVersion(),
                            "--sequence", "1",
                            "--cafile", config.getOrdererTlsCaPem(),
                            isTlsEnabled ? "--tls" : ""
                    };
                    Container.ExecResult result = toUnchecked(
                            () -> container.execInContainer(
                                    ExecConfig.builder()
                                            .user("microfab")
                                            .workDir(workDir)
                                            .command(command)
                                            .envVars(Map.of(
                                                    "FABRIC_LOGGING_SPEC", "debug",
                                                    "FABRIC_CFG_PATH", org.getPeerCfgDir(),
                                                    "CORE_PEER_ADDRESS", config.getApiUrl(org)
                                            ))
                                            .build())
                    );
                    log.info(Arrays.toString(command));
                    log.info(result.toString());
                });
    }

    private static void runLifecycle(MicrofabContainer<?> container, Channel channel, String[] command,
                                     boolean needAdmin) {
        Config config = container.getConfig();
        Set<Config.Organization> validOrganizations = config.getEndorsingOrganizations();
        // FABRIC_LOGGING_SPEC=debug
        // FABRIC_CFG_PATH=/home/microfab/data/peer-mat/config
        // CORE_PEER_ADDRESS=matpeer-api.127-0-0-1.nip.io:8080
        // peer lifecycle chaincode checkcommitreadiness
        // --channelID tokenization
        // --name hello
        // --version 1.0
        // --sequence 1
        // --output json
        Set<String> channelOrganizations = channel.getEndorsingOrganizations();
        validOrganizations.stream()
                .filter(o -> channelOrganizations.contains(o.getName()))
                .forEach(org -> {
                    String workDir = config.getDirectory();
                    Container.ExecResult result = toUnchecked(
                            () -> container.execInContainer(
                                    ExecConfig.builder()
                                            .user("microfab")
                                            .workDir(workDir)
                                            .command(command)
                                            .envVars(
                                                    Stream.concat(
                                                            Map.of(
                                                                    "FABRIC_LOGGING_SPEC", "debug",
                                                                    "FABRIC_CFG_PATH", org.getPeerCfgDir(),
                                                                    "CORE_PEER_ADDRESS", config.getApiUrl(org)
                                                            ).entrySet().stream(),
                                                            needAdmin ? Map.of(
                                                                    "CORE_PEER_MSPCONFIGPATH", workDir + "/" + org.getAdminMspDir()
                                                            ).entrySet().stream() : Stream.empty()
                                                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                                            .build())
                    );
                    log.info(Arrays.toString(command));
                    log.info(result.toString());
                });
    }

    private static WithEnvironmentVariables configureSsl(Config.Tls tls, Chaincode chaincode, WithEnvironmentVariables env) {
        return toUnchecked(
                () -> {
                    Path ccTmpDir = createTempDirectory(chaincode.getName() + "_");
                    Path ca = createTempFile(ccTmpDir, "ca_", ".pem").toAbsolutePath();
                    Path cert = createTempFile(ccTmpDir, "cert_", ".pem").toAbsolutePath();
                    Path base64Cert = createTempFile(ccTmpDir, "cert_", ".base64").toAbsolutePath();
                    Path key = createTempFile(ccTmpDir, "key_", ".pem").toAbsolutePath();
                    Path base64Key = createTempFile(ccTmpDir, "key_", ".base64").toAbsolutePath();

                    writeString(ca, tls.getCaPem());
                    writeString(cert, tls.getCertificatePem());
                    writeString(base64Cert, tls.getCertificate());
                    writeString(key, tls.getPrivateKeyPem());
                    writeString(base64Key, tls.getPrivateKey());

                    return env.and("CORE_PEER_TLS_ENABLED", Boolean.TRUE.toString())
                            .and("CORE_TLS_CLIENT_CERT_PATH", base64Cert.toString())
                            .and("CORE_TLS_CLIENT_KEY_PATH", base64Key.toString())
                            .and("CORE_PEER_TLS_ROOTCERT_FILE", ca.toString())
                            .and("CORE_TLS_CLIENT_CERT_FILE", cert.toString())
                            .and("CORE_TLS_CLIENT_KEY_FILE", key.toString());
                });
    }

    @Override
    public boolean detect(Chaincode chaincode) {
        return chaincode.getType() == Microfab.Chaincode.Type.CCAAS;
    }

    @Override
    public void build(MicrofabContainer<?> container, Channel channel, Chaincode chaincode) {
        _package(container, chaincode);
        install(container, channel, chaincode);
        approveForMyOrg(container, channel, chaincode);
        checkCommitReadiness(container, channel, chaincode);
        commit(container, channel, chaincode);
    }

    @Override
    public void run(MicrofabContainer<?> container, Channel channel, Chaincode chaincode) {
        Config config = container.getConfig();
        toUnchecked(
                () -> {
                    Config.Tls tls = config.getTls();
                    var customizedEnv = withEnvironmentVariable("CHAINCODE_SERVER_ADDRESS", chaincode.getAddress())
                            .and("CORE_CHAINCODE_ID_NAME", chaincode.getPkgId());
                    if (tls.isEnabled()) {
                        customizedEnv = configureSsl(tls, chaincode, customizedEnv);
                    }
                    customizedEnv.execute(() -> {
                        var cr = new ContractRouter(new String[0]);
                        var cs = new NonBlockingChaincodeServer(cr, cr.getChaincodeServerConfig());
                        cr.startRouterWithChaincodeServer(cs);
                    });
                    return null;
                }
        );
    }
}
