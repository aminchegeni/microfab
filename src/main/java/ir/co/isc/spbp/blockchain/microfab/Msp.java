package ir.co.isc.spbp.blockchain.microfab;

import ir.co.isc.spbp.blockchain.microfab.model.Config;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a test field to be injected with a {@link org.hyperledger.fabric.client.Gateway}
 * instance bound to a specific organization’s MSP identity.
 * <p>
 * This annotation is processed by the {@code MicrofabExtension} for JUnit 5.
 * When applied to a field in a test class, the extension will:
 * <ol>
 *   <li>Resolve the MSP identity and connection details for the specified organization
 *       from {@code state.json} and the {@link Config} class.</li>
 *   <li>Construct a {@link org.hyperledger.fabric.client.Gateway} using the
 *       {@link org.hyperledger.fabric.client.identity.Identity} and
 *       {@link org.hyperledger.fabric.client.identity.Signer} for that organization.</li>
 *   <li>Inject the resulting {@code Gateway} instance into the annotated field
 *       before each test executes.</li>
 * </ol>
 *
 * <h3>Example usage</h3>
 * <pre>{@code
 * class MyChaincodeTest {
 *
 *     @Msp(org = "Org1")
 *     private Gateway org1Gateway;
 *
 *     @Test
 *     void queryLedger() throws Exception {
 *         Network network = org1Gateway.getNetwork("mychannel");
 *         Contract contract = network.getContract("mycc");
 *         byte[] result = contract.evaluateTransaction("queryAllAssets");
 *         assertNotNull(result);
 *     }
 * }
 * }</pre>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>The field must be non-final and of type {@link org.hyperledger.fabric.client.Gateway}.</li>
 *   <li>The {@code org} attribute must match an organization identifier present in the
 *       Microfab configuration.</li>
 *   <li>The {@code MicrofabExtension} ensures that gateways are built with valid TLS
 *       connection info, identities, and signers.</li>
 * </ul>
 */
@Target(FIELD)
@Retention(RUNTIME)
@Inherited
public @interface Msp {

    /**
     * The name of the organization whose MSP identity and credentials should be used
     * to create and inject the {@link org.hyperledger.fabric.client.Gateway}.
     *
     * @return the organization identifier (e.g. {@code "Org1"})
     */
    String org();
}
