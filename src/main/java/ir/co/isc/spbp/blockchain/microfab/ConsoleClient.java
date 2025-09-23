package ir.co.isc.spbp.blockchain.microfab;

import ir.co.isc.spbp.blockchain.microfab.model.Config;
import ir.co.isc.spbp.blockchain.microfab.model.State;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ir.co.isc.spbp.blockchain.microfab.utils.Util.toUnchecked;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Client for interacting with the Microfab console REST API.
 * <p>
 * This client connects to the Microfab console's {@code /ak/api/v1/components} endpoint
 * to retrieve information about the available identities. Only non-hidden identities
 * of type {@code identity} are collected and returned.
 * </p>
 *
 * <p>
 * If TLS is enabled in the provided {@link Config}, this client builds an
 * {@link SSLContext} that trusts the single Microfab CA certificate given in the config.
 * Otherwise, a plain HTTP client is used.
 * </p>
 *
 * <p>
 * Typical usage:
 * <pre>{@code
 * try (ConsoleClient client = new ConsoleClient(config)) {
 *     Map<String, State.Identity> identities = client.getIdentities();
 * }
 * }</pre>
 * </p>
 *
 * <p>
 * This class is {@link AutoCloseable} and should be closed after use to free underlying resources.
 * </p>
 */
public class ConsoleClient implements AutoCloseable {

    private final Config config;
    private HttpClient client;

    /**
     * Creates a new console client with the given configuration.
     * The underlying HTTP client is initialized according to the TLS settings.
     *
     * @param config configuration object containing Microfab console connection settings
     */
    public ConsoleClient(Config config) {
        this.config = config;
        init();
    }

    /**
     * Closes the underlying {@link HttpClient}.
     * <p>
     * This method should be invoked when the client is no longer needed
     * to release any associated system resources.
     * </p>
     */
    @Override
    public void close() {
        client.close();
    }

    /**
     * Retrieves all visible identities from the Microfab console.
     * <p>
     * This method queries the {@code /ak/api/v1/components} REST endpoint of the console.
     * For each element in the response:
     * <ul>
     *   <li>If {@code type == "identity"} and {@code hide == false}, it is considered a valid identity.</li>
     *   <li>Each valid identity is converted to a {@link State.Identity} using {@link State.Identity#from(JSONObject)}.</li>
     *   <li>The identities are collected into a map keyed by their unique identifier.</li>
     * </ul>
     *
     * @return a map of identity IDs to {@link State.Identity} objects representing admin identities
     * @throws RuntimeException if an error occurs while communicating with the console
     */
    public Map<String, State.Identity> getAdmins() {
        return toUnchecked(
                () -> {
                    boolean isTls = config.getTls().isEnabled();
                    String domain = config.getDomain();
                    int port = config.getPort();
                    String address = "http%s://console.%s:%d/ak/api/v1/components".formatted(isTls ? "s" : "", domain, port);
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(address))
                            .GET()
                            .build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    Stream.Builder<State.Identity> identities = Stream.builder();
                    if (resp.statusCode() == 200) {
                        JSONArray json = new JSONArray(resp.body());
                        for (int i = 0; i < json.length(); i++) {
                            JSONObject element = json.getJSONObject(i);
                            boolean isIdentity = "identity".equals(element.optString("type", ""));
                            boolean isHide = element.optBoolean("hide", true);
                            if (isIdentity && !isHide) {
                                identities.add(State.Identity.from(element));
                            }
                        }
                    }
                    return identities.build().collect(Collectors.toMap(State.Identity::getId, Function.identity()));
                });
    }

    /**
     * Initializes the underlying {@link HttpClient} according to the TLS settings
     * defined in the provided {@link Config}.
     * <p>
     * If TLS is enabled, a custom {@link SSLContext} trusting only the Microfab CA
     * certificate is configured. Otherwise, a default HTTP client is used.
     * </p>
     */
    private void init() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (config.getTls().isEnabled()) {
            builder.sslContext(createSSLContext());
        }
        client = builder.build();
    }

    /**
     * Creates an {@link SSLContext} that trusts only the Microfab CA certificate
     * provided in the {@link Config}.
     * <p>
     * The CA certificate is loaded from the config, inserted into an in-memory
     * {@link KeyStore}, and used to initialize a {@link TrustManagerFactory}.
     * The resulting {@link SSLContext} can be used to securely connect to
     * the Microfab console over HTTPS.
     * </p>
     *
     * @return an initialized {@link SSLContext} trusting only the Microfab CA certificate
     * @throws RuntimeException if an error occurs while creating the SSL context
     */
    private SSLContext createSSLContext() {
        return toUnchecked(
                () -> {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    Certificate cert = cf.generateCertificate(new ByteArrayInputStream(config.getTls().getCaPem().getBytes(UTF_8)));
                    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    ks.load(null, null); // initialize empty keystore
                    ks.setCertificateEntry("*." + config.getDomain(), cert);
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(ks);
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(new KeyManager[0], tmf.getTrustManagers(), null);
                    return sslContext;
                });
    }
}
