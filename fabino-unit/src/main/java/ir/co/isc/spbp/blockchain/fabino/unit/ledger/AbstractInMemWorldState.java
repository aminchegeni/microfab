package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.synchronizedMap;

/**
 * Abstract in-memory implementation of {@link WorldState} that provides
 * a concrete key–value storage and basic query behavior.
 * <p>
 * This class encapsulates the shared mechanics of an in-memory world state:
 * storing values, applying updates, removing keys, and exposing snapshots.
 * Subclasses are expected to differentiate themselves only by their
 * declared {@link WorldState.Type}.
 *
 * <h2>Storage Model</h2>
 * <p>
 * State is maintained in a {@link ConcurrentHashMap} where each key maps
 * to its current value. Only the latest value is retained; no historical
 * information is preserved.
 *
 * <h2>Concurrency Characteristics</h2>
 * <p>
 * The underlying map supports concurrent access, allowing reads and
 * writes to occur safely in typical test scenarios without additional
 * synchronization.
 *
 * <h2>Query Behavior</h2>
 * <p>
 * Queries are implemented as simple prefix matches over the current key
 * set. The result reflects the state at the time of invocation and does
 * not imply any ordering, indexing, or pagination guarantees.
 *
 * <p>
 * This class intentionally does not interpret or enforce any semantics
 * beyond basic key–value state management.
 *
 * @see WorldState
 */
public abstract class AbstractInMemWorldState implements WorldState {

    /**
     * The internal key-value storage representing the current world state.
     * <p>
     * Keys and values represent the latest committed state only.
     */
    protected final Map<String, String> states = synchronizedMap(new TreeMap<>());

    /**
     * Retrieves the current value associated with the given key.
     *
     * @param key the world state key
     * @return the stored value, or {@code null} if the key does not exist
     */
    @Override
    public String get(String key) {
        return states.get(key);
    }

    /**
     * Inserts or updates the value associated with the given key.
     *
     * @param key   the world state key
     * @param value the value to associate with the key
     */
    @Override
    public void put(String key, String value) {
        states.put(key, value);
    }

    /**
     * Removes the given key from the world state.
     *
     * @param key the world state key to remove
     */
    @Override
    public void delete(String key) {
        states.remove(key);
    }

    /**
     * Performs a prefix-based query over the world state.
     * <p>
     * Only keys currently present in the world state are considered.
     * Historical values are not available.
     *
     * @param keyPrefix the key prefix to match
     * @return a list of matching key-value entries
     */
    @Override
    public List<Map.Entry<String, String>> query(String keyPrefix) {
        return states.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(keyPrefix))
                .toList();
    }

    /**
     * Returns an immutable snapshot of the current world state.
     * <p>
     * The returned map represents a point-in-time view of the state and
     * must not be modified by the caller.
     *
     * @return an immutable copy of the world state
     */
    @Override
    public Map<String, String> snapshot() {
        return Map.copyOf(states);
    }
}
