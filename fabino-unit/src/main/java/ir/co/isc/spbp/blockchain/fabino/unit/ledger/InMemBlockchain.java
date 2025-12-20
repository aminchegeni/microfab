package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

import java.util.*;

import static java.util.Collections.synchronizedList;

/**
 * In-memory implementation of the {@link Blockchain} interface intended
 * for unit and integration testing.
 * <p>
 * This implementation maintains an append-only, ordered list of
 * {@link Block} instances entirely in process memory. It provides a
 * deterministic and lightweight representation of blockchain history
 * without persistence, consensus, or validation semantics.
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Internally, blocks are stored in a {@link Collections#synchronizedList(List)}}
 * to provide basic thread-safety for concurrent append and read operations.
 * <p>
 * While this allows safe concurrent access in test environments, this
 * class is <em>not</em> designed for high-throughput or highly concurrent
 * workloads. Its synchronization model is intentionally simple.
 *
 * <h2>Behavioral Characteristics</h2>
 * <ul>
 *   <li>Blocks are appended in the order received.</li>
 *   <li>Blocks are never removed or modified once added.</li>
 *   <li>Returned block collections are defensive copies.</li>
 *   <li>Out-of-range block access is handled gracefully.</li>
 * </ul>
 *
 * <h2>Differences from Real Hyperledger Fabric</h2>
 * <ul>
 *   <li>No persistence to disk or database.</li>
 *   <li>No block validation or transaction filtering.</li>
 *   <li>No cryptographic linkage between blocks.</li>
 *   <li>No ordering service or channel isolation.</li>
 * </ul>
 *
 * <p>
 * This class is typically used by a mock {@code Ledger} implementation,
 * which combines this blockchain history with a {@code WorldState}
 * abstraction to emulate Fabric ledger behavior for tests.
 *
 * @see Blockchain
 * @see Block
 * @see <a href="https://hyperledger-fabric.readthedocs.io/en/latest/ledger/ledger.html#the-ledger">
 *      Hyperledger Fabric Ledger Documentation</a>
 */
public class InMemBlockchain implements Blockchain {

    /**
     * The internal, append-only list of blocks.
     * <p>
     * The list is synchronized to allow safe concurrent access during
     * test execution.
     */
    private final List<Block> blocks = synchronizedList(new ArrayList<>(16));

    /**
     * Appends a block to the end of the blockchain.
     *
     * @param block the block to append
     */
    @Override
    public void append(Block block) {
        blocks.add(block);
    }

    /**
     * Returns an immutable snapshot of all blocks in the blockchain.
     *
     * @return an ordered, immutable list of blocks
     */
    @Override
    public List<Block> getBlocks() {
        return List.copyOf(blocks);
    }

    /**
     * Retrieves a block by its index.
     * <p>
     * If the index is out of range, an empty {@link Optional} is returned
     * instead of throwing an exception.
     *
     * @param idx the block index
     * @return an {@code Optional} containing the block if present
     */
    @Override
    public Optional<Block> getBlock(int idx) {
        try {
            Objects.checkIndex(idx, blocks.size());
            return Optional.of(blocks.get(idx));
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }
}
