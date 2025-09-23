package ir.co.isc.spbp.blockchain.microfab.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.json.JSONPropertyName;

import static ir.co.isc.spbp.blockchain.microfab.model.Config.Chaincode;

/**
 * Represents the {@code connection.json} file used in a chaincode package
 * for Chaincode-as-a-Service (CCaaS) deployment in Hyperledger Fabric.
 * <p>
 * This structure mirrors the Go {@code connection} struct defined in
 * <a href="https://github.com/hyperledger/fabric/blob/main/ccaas_builder/cmd/build/main.go#L62-L70">
 * ccaas_builder</a>, which uses these fields:
 * <pre>{@code
 * type connection struct {
 *   Address     string `json:"address"`
 *   DialTimeout string `json:"dial_timeout"`
 *   TLS         bool   `json:"tls_required"`
 *   ClientAuth  bool   `json:"client_auth_required"`
 *   RootCert    string `json:"root_cert"`
 *   ClientKey   string `json:"client_key"`
 *   ClientCert  string `json:"client_cert"`
 * }
 * }</pre>
 * <p>
 * Use this class to serialize / deserialize the JSON so your Java side stays
 * compatible with the peer’s expectations for external/CCaaS chaincode connection.
 */
@Data
@Builder(builderClassName = "Builder")
public class Connection implements Jsonable {

    /**
     * The address (host:port) where the chaincode server is running.
     * <p>Example: {@code "host.docker.internal:9999"}</p>
     */
    @lombok.Builder.Default
    private String address = "host.docker.internal:9999";

    /**
     * Dial timeout duration as a string (e.g. {@code "10s"}).
     * Controls how long the peer waits when establishing a connection
     * to the chaincode server.
     */
    @Getter(onMethod_ = @JSONPropertyName("dial_timeout"))
    private String dialTimeout;

    /**
     * Indicates whether TLS is required when connecting
     * from the peer to the chaincode service.
     */
    @Getter(onMethod_ = @JSONPropertyName("tls_required"))
    private boolean tls;

    /**
     * Indicates whether client authentication is required
     * (i.e., the chaincode service requires the peer to
     * present a client certificate).
     */
    @Getter(onMethod_ = @JSONPropertyName("client_auth_required"))
    private boolean clientAuth;

    /**
     * The PEM-encoded root certificate used to verify the TLS
     * certificate presented by the chaincode server.
     */
    @Getter(onMethod_ = @JSONPropertyName("root_cert"))
    private String rootCert;

    /**
     * The PEM-encoded client private key, used when
     * {@link #clientAuth} is enabled and the peer must
     * authenticate to the chaincode server.
     */
    @Getter(onMethod_ = @JSONPropertyName("client_key"))
    private String clientKey;

    /**
     * The PEM-encoded client certificate, used together with
     * {@link #clientKey} when {@link #clientAuth} is enabled.
     */
    @Getter(onMethod_ = @JSONPropertyName("client_cert"))
    private String clientCert;

    /**
     * Factory method to create a {@link Connection} instance from a {@link Config} and
     * a {@link Chaincode} annotation.
     * <p>
     * This method extracts the network/peer information from the {@code Config} instance
     * and the chaincode name/type/address from the {@code Chaincode} annotation, and
     * populates the {@link Connection} fields accordingly.
     *
     * @param config    the Microfab {@link Config} instance containing peer, TLS, and CA info
     * @param chaincode the {@link Chaincode} annotation instance describing the chaincode
     * @return a new {@link Connection} populated with values from both {@code config} and {@code chaincode}
     */
    public static Connection of(Config config, Chaincode chaincode) {
        Config.Tls tls = config.getTls();
        return Connection.builder()
                .address(chaincode.getAddress())
                .dialTimeout(config.getTimeout())
                .tls(tls.isEnabled())
                .clientAuth(false)
                .rootCert(tls.getCaPem())
                .clientKey(tls.getPrivateKeyPem())
                .clientCert(tls.getCertificatePem())
                .build();
    }
}
