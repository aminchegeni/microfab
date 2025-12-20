package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

import java.util.List;
import java.util.Optional;

/**
 * Represents a simplified, append-only blockchain abstraction for use in
 * mock Hyperledger Fabric ledger implementations.
 * <p>
 * In Hyperledger Fabric, the blockchain is an immutable, ordered sequence
 * of blocks, where each block contains a batch of validated transactions
 * that have been committed through the ordering service and consensus
 * mechanisms.
 * <p>
 * This interface models that concept in its most restrained form:
 * an in-memory, deterministic sequence of {@link Block} instances intended
 * for unit and integration testing. It is designed to be used in conjunction
 * with a {@code Ledger} abstraction that combines both the blockchain history
 * and the {@code WorldState}.
 *
 * <h2>Role in the Mock Ledger Architecture</h2>
 * <ul>
 *   <li>Maintains the immutable history of committed blocks.</li>
 *   <li>Provides ordered access to blocks by index.</li>
 *   <li>Serves as the historical record backing the {@code WorldState}.</li>
 *   <li>Enables test assertions over block contents and ordering.</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li>Blocks are append-only and never modified after insertion.</li>
 *   <li>No consensus, validation, or endorsement logic is modeled.</li>
 *   <li>No forks, reorgs, or distributed drama are supported.</li>
 *   <li>All operations are deterministic and synchronous.</li>
 * </ul>
 *
 * <h2>Differences from Real Hyperledger Fabric</h2>
 * <ul>
 *   <li>No block metadata, headers, or previous-hash linkage.</li>
 *   <li>No persistence beyond process memory.</li>
 *   <li>No ordering service or channel isolation.</li>
 *   <li>No transaction validation codes.</li>
 * </ul>
 *
 * <p>
 * This abstraction intentionally avoids simulating Fabric’s internal
 * complexity. Its sole purpose is to provide a predictable and test-friendly
 * representation of blockchain history.
 *
 * @see Block
 * @see Transaction
 * @see <a href="https://hyperledger-fabric.readthedocs.io/en/latest/ledger/ledger.html#the-ledger">
 *      Hyperledger Fabric Ledger Documentation</a>
 */
public interface Blockchain {

    /**
     * Appends a new block to the end of the blockchain.
     * <p>
     * Blocks are added in strict sequential order and are assumed to be
     * fully formed and immutable at the time of insertion.
     *
     * @param block the block to append to the blockchain
     */
    void append(Block block);

    /**
     * Returns the complete list of blocks in the blockchain.
     * <p>
     * The returned list represents the blockchain history in commit order.
     * Callers should treat the list as read-only.
     *
     * @return an ordered list of all committed blocks
     */
    List<Block> getBlocks();

    /**
     * Retrieves a block by its index.
     * <p>
     * The index corresponds to the block number within the blockchain.
     * If no block exists at the given index, an empty {@link Optional}
     * is returned.
     *
     * @param idx the block index (block number)
     * @return an {@code Optional} containing the block if present
     */
    Optional<Block> getBlock(int idx);
}
