package ir.co.isc.spbp.blockchain.microfab.model;

import ir.co.isc.spbp.blockchain.microfab.Microfab;
import lombok.Builder;
import lombok.Data;

import static ir.co.isc.spbp.blockchain.microfab.model.Config.Chaincode;

/**
 * Represents the {@code metadata.json} file that is packaged alongside
 * a chaincode in Hyperledger Fabric.
 * <p>
 * The metadata file is required for chaincode-as-a-service (CCaaS) packaging
 * and provides basic descriptive information about the chaincode
 * implementation.
 *
 * <h2>Structure</h2>
 * <ul>
 *   <li>{@link #type} → The chaincode type (e.g., {@code ccaas}, {@code java}).</li>
 *   <li>{@link #label} → A human-readable label used to uniquely identify the
 *       chaincode package, conventionally composed of
 *       {@code @Chaincode.name() + "_" + @Chaincode.version()}.</li>
 * </ul>
 *
 * <h2>Example {@code metadata.json}</h2>
 * <pre>{@code
 * {
 *   "type": "ccaas",
 *   "label": "asset-transfer_1.0"
 * }
 * }</pre>
 *
 * <h2>Defaults</h2>
 * <ul>
 *   <li>{@link #type} → {@code ccaas} (from {@link Microfab.Chaincode.Type#CCAAS}).</li>
 *   <li>{@link #label} → {@code "test-cc_1.0"} (from default {@link Chaincode} values).</li>
 * </ul>
 *
 * @see Chaincode
 */
@Data
@Builder(builderClassName = "Builder")
public class Metadata implements Jsonable {

    /**
     * The type of chaincode being packaged.
     * <p>
     * This determines how the peer should interpret and run the chaincode.
     * Common values include:
     * <ul>
     *   <li>{@code ccaas} → Chaincode-as-a-Service</li>
     *   <li>{@code java} → In-process Java chaincode</li>
     * </ul>
     *
     * @default "ccaas"
     */
    @lombok.Builder.Default
    private String type = Microfab.Chaincode.Type.CCAAS.getName();

    /**
     * The package label, used to uniquely identify a chaincode package.
     * <p>
     * By convention, this is derived from the chaincode annotation:
     * {@code @Chaincode.name() + "_" + @Chaincode.version()}.
     * <br>
     * Example: if {@code name="asset-transfer"} and {@code version="1.0"},
     * the label will be {@code "asset-transfer_1.0"}.
     *
     * @default "test-cc_1.0"
     */
    @lombok.Builder.Default
    private String label = "test-cc_1.0";

    // ------------------------------------------------------------------------
    // factory method
    // ------------------------------------------------------------------------

    /**
     * Factory method to create a {@link Metadata} instance directly from a
     * {@link Chaincode} annotation.
     * <p>
     * The resulting {@code Metadata} object uses:
     * <ul>
     *   <li>{@link Chaincode#getType()} → {@link Metadata#type}</li>
     *   <li>{@link Chaincode#getName()} + "_" + {@link Chaincode#getVersion()} → {@link Metadata#label}</li>
     * </ul>
     *
     * @param chaincode the {@link Chaincode} annotation instance
     * @return a new {@link Metadata} populated with values derived from the annotation
     */
    public static Metadata of(Chaincode chaincode) {
        return Metadata.builder()
                .type(chaincode.getType().getName())
                .label(chaincode.getName() + "_" + chaincode.getVersion())
                .build();
    }
}
