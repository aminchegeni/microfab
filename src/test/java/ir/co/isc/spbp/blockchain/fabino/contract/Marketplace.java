package ir.co.isc.spbp.blockchain.fabino.contract;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.stream.StreamSupport;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static org.hyperledger.fabric.contract.annotation.Transaction.TYPE.EVALUATE;
import static org.hyperledger.fabric.contract.annotation.Transaction.TYPE.SUBMIT;

/**
 * <h2>🎨 Artino Marketplace Smart Contract</h2>
 * <p>
 * The <strong>Marketplace</strong> contract provides CRUD and query functionality
 * for managing {@link Artwork} assets stored on the Hyperledger Fabric ledger.
 * It is designed as a demonstration contract to illustrate:
 *
 * <ul>
 *     <li>Usage of <strong>Fabino</strong> validation with annotations</li>
 *     <li>Composite key strategies for indexing and efficient querying</li>
 *     <li>How to build Fabric <em>Submit</em> and <em>Evaluate</em> transactions</li>
 *     <li>JSON-based serialization and deserialization</li>
 * </ul>
 *
 * <p>
 * This contract serves as a learning and testing reference for developers
 * adopting Fabric smart contract patterns and exploring validation-driven
 * data models and ledger interoperability using structured types.
 * </p>
 *
 * <h3>🔑 Composite Key Format</h3>
 * Each stored artwork is indexed using the key pattern:
 * <pre>{@code artino:<category>:<id>}</pre>
 * This design enables high-performance category-based range queries using Fabric-level indexing.
 *
 * @see Artwork
 */
@Slf4j
@Default
@Contract(
        name = "Artino",
        info = @Info(
                title = "Test Artwork Marketplace Contract",
                description = "A demo contract showcasing Fabino DataTypes, validation, and ledger operations.",
                version = "0.1.0",
                contact = @Contact(
                        name = "Amin Chegeni Zadeh",
                        email = "a_chegeni@isc.co.ir",
                        url = "https://github.com/aminchegeni"
                ),
                license = @License(
                        name = "Apache-2.0 License",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        )
)
public class Marketplace implements ContractInterface {

    /**
     * Shared Gson instance for JSON serialization and deserialization.
     */
    private static final Gson GSON = new Gson();

    /**
     * Namespace prefix used to construct composite keys for stored assets.
     */
    private static final String KEY_NS = "artino";

    /**
     * Determines whether the ledger query result represents an existing asset.
     *
     * @param result raw ledger bytes
     * @return true if object exists, false otherwise
     */
    private static boolean exists(byte[] result) {
        return nonNull(result) && result.length > 0;
    }

    /**
     * Creates and stores a new {@link Artwork} asset.
     *
     * <p>
     * The ledger key is generated using a composite structure:
     * <pre>{@code artino:<category>:<id>}</pre>
     * If an asset with the same key already exists, the method throws
     * a {@link ChaincodeException}.
     * </p>
     *
     * <h4>Validation Notes</h4>
     * The incoming {@code artwork} instance is validated based on constraints
     * specified inside the {@link Artwork} class using schema annotations {@link Property @Property}.
     *
     * @param ctx     Fabric transaction context
     * @param artwork fully formed artwork supplied by a client application
     * @return stored artwork instance
     */
    @Transaction(name = "CreateArtwork", intent = SUBMIT)
    public Artwork createArtwork(final Context ctx, final Artwork artwork) {
        ChaincodeStub stub = ctx.getStub();
        String key = stub.createCompositeKey(KEY_NS, artwork.getCategory(), valueOf(artwork.getId())).toString();
        byte[] result = stub.getState(key);

        if (exists(result)) {
            throw new ChaincodeException("artwork already exists", GSON.toJson(artwork));
        } else {
            stub.putStringState(key, GSON.toJson(artwork));
        }

        return artwork;
    }

    /**
     * Reads an {@link Artwork} from the ledger.
     *
     * <p>
     * Parameters are subject to schema validation via {@link Property @Property}:
     * </p>
     * <ul>
     *     <li><strong>id</strong>: must match {@code ^[1-9][0-9]{6}$}</li>
     *     <li><strong>category</strong>: must match one of the predefined {@code enum} values</li>
     * </ul>
     *
     * <p>
     * If no matching asset is found, a {@link ChaincodeException} is thrown and
     * includes metadata for debugging purposes.
     * </p>
     *
     * @return the stored asset
     */
    @Transaction(name = "ReadArtwork", intent = EVALUATE)
    public Artwork readArtwork(final Context ctx,
                               @Property(schema = {
                                       "title", "Artwork Identifier",
                                       "pattern", "^[1-9][0-9]{6}$"
                               }) final String id,
                               @Property(schema = {
                                       "title", "Artwork Category",
                                       "pattern", "^(painting|video|music|photography|animation)$"
                               }) final String category) {

        ChaincodeStub stub = ctx.getStub();
        String key = stub.createCompositeKey(KEY_NS, category, id).toString();
        byte[] result = stub.getState(key);

        if (!exists(result)) {
            Artwork missing = new Artwork();
            missing.setCategory(category);
            missing.setId(parseInt(id));
            throw new ChaincodeException("artwork doesn't exists", GSON.toJson(missing));
        }

        return GSON.fromJson(new String(result, UTF_8), Artwork.class);
    }

    /**
     * Updates an existing artwork in full.
     *
     * <p>
     * The current record must exist; otherwise, a {@link ChaincodeException} is thrown.
     * This method replaces the asset entirely rather than performing partial updates.
     * </p>
     * <p>
     * transaction type {@link Transaction.TYPE#SUBMIT}
     */
    @Transaction(name = "UpdateArtwork", intent = SUBMIT)
    public Artwork updateArtwork(final Context ctx, final Artwork artwork) {
        ChaincodeStub stub = ctx.getStub();
        String key = stub.createCompositeKey(KEY_NS, artwork.getCategory(), valueOf(artwork.getId())).toString();
        byte[] result = stub.getState(key);

        if (!exists(result)) {
            throw new ChaincodeException("artwork doesn't exists", GSON.toJson(artwork));
        } else {
            stub.putStringState(key, GSON.toJson(artwork));
        }

        return artwork;
    }

    /**
     * Deletes a stored artwork from the ledger.
     *
     * <p>
     * Parameters are subject to schema validation via {@link Property @Property}:
     * </p>
     * <ul>
     *     <li><strong>id</strong>: must match {@code ^[1-9][0-9]{6}$}</li>
     *     <li><strong>category</strong>: must match one of the predefined {@code enum} values</li>
     * </ul>
     *
     * <p>
     * This is an irreversible destructive operation.
     * </p>
     */
    @Transaction(name = "DeleteArtwork", intent = SUBMIT)
    public void deleteArtwork(final Context ctx,
                              @Property(schema = {
                                      "title", "Artwork Identifier",
                                      "pattern", "^[1-9][0-9]{6}$"
                              }) final String id,
                              @Property(schema = {
                                      "title", "Artwork Category",
                                      "pattern", "^(painting|video|music|photography|animation)$"
                              }) final String category) {

        ChaincodeStub stub = ctx.getStub();
        String key = stub.createCompositeKey(KEY_NS, category, id).toString();
        stub.delState(key);
    }

    /**
     * Retrieves all artworks that belong to a specific category using a
     * <strong>partial composite key query</strong>. This form of query allows returning
     * all ledger entries whose keys begin with a shared prefix, without requiring the
     * full composite key (such as the asset ID).
     *
     * <p>The underlying key structure used by this contract follows the format:
     * <pre>{@code
     * artino:<category>:<id>
     * }</pre>
     *
     * <h3>Example: Partial Query Scenario</h3>
     * Suppose the ledger contains the following composite keys:
     * <pre>{@code
     * artino:video:1000396
     * artino:video:1000397
     * artino:video:abc123
     * artino:video:anythingElse
     * artino:painting:1000401
     * artino:music:1003300
     * }</pre>
     *
     * <p>When invoking this method:
     *
     * <pre>{@code
     * queryBy(ctx, "video");
     * }</pre>
     * <p>
     * The Fabric peer performs a prefix search.
     *
     * <p>The effective lookup prefix sent to Fabric will be:
     * <pre>{@code
     * artino:video
     * }</pre>
     *
     * <p><strong>Matching entries returned:</strong>
     * <pre>{@code
     * artino:video:1000396
     * artino:video:1000397
     * artino:video:abc123
     * artino:video:anythingElse
     * }</pre>
     *
     * <p><strong>Excluded entries:</strong>
     * <pre>{@code
     * artino:painting:1000401   // different category prefix
     * artino:music:1003300      // different category prefix
     * }</pre>
     *
     * <p>This technique is often used instead of a full ledger scan because:
     * <ul>
     *     <li>It is efficient and indexed by Fabric's key structure.</li>
     *     <li>It avoids writing rich JSON queries for common lookups.</li>
     *     <li>It scales well when thousands (or millions) of assets share a key prefix.</li>
     * </ul>
     *
     * @param ctx      Fabric transaction context.
     * @param category Category filter value. Must match one of:
     *                 {@code painting|video|music|photography|animation}
     * @return A list of {@link Artwork} objects whose keys begin with the specified category prefix.
     * The list may be empty, but never {@code null}.
     * @see ChaincodeStub#getStateByPartialCompositeKey(CompositeKey) for Fabric lookup implementation details.
     */
    @Transaction(name = "QueryBy", intent = EVALUATE)
    public Artwork[] queryBy(final Context ctx,
                                 @Property(schema = {
                                         "title", "Artwork Category",
                                         "pattern", "^(painting|video|music|photography|animation)$"
                                 }) final String category) {

        ChaincodeStub stub = ctx.getStub();
        String key = stub.createCompositeKey(KEY_NS, category).toString();
        QueryResultsIterator<KeyValue> results = stub.getStateByPartialCompositeKey(key);

        return StreamSupport.stream(results.spliterator(), false)
                .map(KeyValue::getStringValue)
                .map(v -> GSON.fromJson(v, Artwork.class))
                .toArray(Artwork[]::new);
    }

    /**
     * Updates ownership of an existing asset.
     *
     * <p>
     * This demonstrates a partial state mutation pattern
     * where only a single attribute is updated instead of full replacement.
     * </p>
     *
     * <h4>Input validation rules:</h4>
     * <ul>
     *     <li><strong>id:</strong> {@code ^[1-9][0-9]{6}$}</li>
     *     <li><strong>category</strong>: must match one of the predefined {@code enum} values</li>
     *     <li><strong>owner:</strong> {@code ^[A-Z0-9]{10}$}</li>
     * </ul>
     *
     * @return updated artwork instance
     */
    @Transaction(name = "ChangeOwner", intent = SUBMIT)
    public Artwork ChangeOwner(final Context ctx,
                               @Property(schema = {
                                       "title", "Artwork Identifier",
                                       "pattern", "^[1-9][0-9]{6}$"
                               }) final String id,
                               @Property(schema = {
                                       "title", "Artwork Category",
                                       "pattern", "^(painting|video|music|photography|animation)$"
                               }) final String category,
                               @Property(schema = {
                                       "title", "Owner Identifier",
                                       "pattern", "^[A-Z0-9]{10}$"
                               }) final String owner) {

        ChaincodeStub stub = ctx.getStub();
        String key = stub.createCompositeKey(KEY_NS, category, id).toString();
        byte[] result = stub.getState(key);

        if (!exists(result)) {
            Artwork missing = new Artwork();
            missing.setCategory(category);
            missing.setId(parseInt(id));
            throw new ChaincodeException("artwork doesn't exists", GSON.toJson(missing));
        } else {
            Artwork artwork = GSON.fromJson(new String(result, UTF_8), Artwork.class);
            artwork.setOwner(owner);
            stub.putStringState(key, GSON.toJson(artwork));
            return artwork;
        }
    }
}
