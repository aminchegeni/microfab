package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Represents a simplified, in-memory abstraction of the Hyperledger Fabric
 * <em>World State</em>, intended for deterministic unit and integration testing.
 * <p>
 * In Hyperledger Fabric, the ledger is composed of two logically distinct parts:
 * <ul>
 *   <li><strong>The Blockchain</strong>, which is an immutable, append-only
 *       sequence of blocks containing transaction history</li>
 *   <li><strong>The World State</strong>, which is a database holding the
 *       <em>latest committed value</em> for each key</li>
 * </ul>
 * <p>
 * This interface models only the <strong>World State</strong> portion of the
 * ledger, deliberately omitting historical data, transaction validation,
 * MVCC versioning, and endorsement semantics. Its purpose is to provide a
 * minimal and predictable state store suitable for mocking ledger behavior
 * in tests.
 *
 * <p>
 * In a typical test setup, a {@code ChaincodeStubMock} delegates read and write
 * operations to a {@code Ledger} abstraction. That {@code Ledger} encapsulates
 * both a {@code Blockchain} (for block-level inspection) and a
 * {@code WorldState} (for key-value access). This interface defines the contract
 * for that world state component.
 *
 * <h2>Conceptual Mapping to Hyperledger Fabric</h2>
 * <ul>
 *   <li>Each key maps to its <em>most recent committed value</em>.</li>
 *   <li>Updates overwrite previous values atomically.</li>
 *   <li>Deletes remove the key entirely from the state.</li>
 *   <li>Queries operate only on the current state, never on historical data.</li>
 * </ul>
 *
 * <h2>Behavioral Characteristics</h2>
 * <ul>
 *   <li>No MVCC or version conflict checks are performed.</li>
 *   <li>No transaction context, isolation, or rollback exists.</li>
 *   <li>All operations are assumed to be immediately committed.</li>
 *   <li>Values are stored as raw bytes to resemble Fabric’s internal model.</li>
 * </ul>
 *
 * <p>
 * Implementations are expected to be deterministic and free of external
 * side effects, ensuring that repeated test executions produce identical
 * results—an attribute highly prized by both engineers and coffee machines.
 *
 * @see <a href="https://hyperledger-fabric.readthedocs.io/en/latest/ledger/ledger.html#the-ledger">
 * Hyperledger Fabric Ledger Documentation</a>
 */
public interface WorldState {

    /**
     * Retrieves the current value associated with the given key.
     * <p>
     * This method returns the <em>latest committed value</em> for the specified
     * key as stored in the world state. If the key does not exist, {@code null}
     * is returned.
     * <p>
     * This mirrors the behavior of {@code GetState} in the Fabric chaincode API,
     * without transaction context or read-set tracking.
     *
     * @param key the world state key
     * @return the stored value, or {@code null} if the key is not present
     */
    String get(String key);

    /**
     * Inserts or updates the value associated with the given key.
     * <p>
     * If the key already exists, its value is replaced. If the key does not
     * exist, a new entry is created in the world state.
     * <p>
     * This operation is conceptually equivalent to {@code PutState} in
     * Hyperledger Fabric, but without endorsement, validation, or versioning.
     *
     * @param key   the world state key
     * @param value the value to associate with the key
     */
    void put(String key, String value);

    /**
     * Removes the given key and its value from the world state.
     * <p>
     * If the key does not exist, this operation has no effect.
     * <p>
     * This models the behavior of {@code DelState} in Fabric, immediately
     * removing the key from the current state view.
     *
     * @param key the world state key to remove
     */
    void delete(String key);

    /**
     * Performs a simple prefix-based query over the world state.
     * <p>
     * This method returns all key-value entries whose keys start with the
     * specified prefix. The returned data represents a snapshot of the
     * world state at the time of invocation.
     * <p>
     * This loosely mimics Fabric range queries or partial composite key
     * queries, but intentionally excludes pagination, indexing, selectors,
     * and rich query semantics.
     *
     * @param keyPrefix the key prefix to match
     * @return a list of matching key-value entries
     */
    List<Map.Entry<String, String>> query(String keyPrefix);

    /**
     * Returns a snapshot of the entire world state.
     * <p>
     * The returned map represents the current contents of the world state
     * and must not be modified by the caller. Implementations should return
     * either an immutable view or a defensive copy.
     * <p>
     * This operation is primarily intended for assertions in tests,
     * debugging, or inspection of ledger state after simulated transactions.
     *
     * @return a snapshot of the current world state
     */
    Map<String, String> snapshot();

    /**
     * Returns the logical type of the underlying world state implementation.
     * <p>
     * This allows tests or higher-level abstractions to adjust behavior
     * depending on whether the world state is intended to resemble
     * LevelDB, CouchDB, or an unknown implementation.
     *
     * @return the world state type
     */
    Type getType();

    /**
     * Enumerates the logical types of world state implementations.
     * <p>
     * In real Hyperledger Fabric deployments, the world state is backed by
     * either LevelDB or CouchDB. This enumeration allows mock implementations
     * to declare which behavior they conceptually resemble.
     */
    @Getter
    enum Type {

        /**
         * Unknown or unspecified world state type.
         * <p>
         * No assumptions should be made about query capabilities.
         */
        UNKNOWN(false),

        /**
         * LevelDB-backed world state.
         * <p>
         * Supports simple key-based access and range queries, but does not
         * support rich JSON queries.
         */
        LEVELDB(false),

        /**
         * CouchDB-backed world state.
         * <p>
         * Supports rich JSON queries and secondary indexes.
         */
        COUCHDB(true);

        /**
         * Indicates whether rich query semantics are supported.
         */
        private final boolean richQuerySupported;

        Type(boolean richQuerySupported) {
            this.richQuerySupported = richQuerySupported;
        }
    }
}
