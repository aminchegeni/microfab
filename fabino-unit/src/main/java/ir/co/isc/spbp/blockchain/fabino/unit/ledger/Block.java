package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

import lombok.Value;

import java.util.HexFormat;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents a simplified, immutable block in a mock Hyperledger Fabric
 * blockchain.
 * <p>
 * In Hyperledger Fabric, a block is an ordered container of transactions
 * that is appended to the blockchain after validation and consensus.
 * Each block is identified by a block number and contains a sequence
 * of committed transactions.
 * <p>
 * This class provides a minimal in-memory representation of that concept,
 * intended exclusively for unit and integration testing where full Fabric
 * block semantics would be excessive, dramatic, and frankly unnecessary.
 *
 * <h2>Role in the Mock Ledger Architecture</h2>
 * <ul>
 *   <li>A {@code Block} groups multiple {@link Transaction} instances.</li>
 *   <li>Blocks are appended sequentially to a {@code Blockchain}.</li>
 *   <li>The block number represents commit order, starting from zero.</li>
 *   <li>The {@code WorldState} is derived by replaying transactions in
 *       block order.</li>
 * </ul>
 *
 * <h2>Block Hash Semantics</h2>
 * <p>
 * In real Hyperledger Fabric, a block hash is derived from block headers,
 * data hashes, and metadata, providing cryptographic integrity and
 * immutability guarantees.
 * <p>
 * In this mock implementation, {@link #getHash()} returns a deterministic
 * hexadecimal value derived from the string representation of the block’s
 * transactions. This provides:
 * <ul>
 *   <li>Stable and repeatable results for tests</li>
 *   <li>A convenient identifier for debugging and assertions</li>
 *   <li>No actual cryptographic security (because tests deserve honesty)</li>
 * </ul>
 *
 * <h2>Simplifications Compared to Real Fabric Blocks</h2>
 * <ul>
 *   <li>No block header or metadata structures.</li>
 *   <li>No previous-hash linkage.</li>
 *   <li>No transaction validation flags.</li>
 *   <li>No ordering service signatures.</li>
 * </ul>
 *
 * <p>
 * The class is immutable and thread-safe, making it suitable for deterministic
 * testing scenarios where blocks should never mutate once committed.
 *
 * @see Transaction
 */
@Value
public class Block {

    /**
     * The sequential block number.
     * <p>
     * This value represents the position of the block within the blockchain
     * and increases monotonically as blocks are appended.
     */
    int number;

    /**
     * The list of transactions contained in this block.
     * <p>
     * Transactions are ordered and are assumed to be committed and valid.
     */
    List<Transaction> transactions;

    /**
     * Returns a deterministic hexadecimal hash representing this block.
     * <p>
     * The hash is derived from the string representation of the block’s
     * transactions and encoded as UTF-8 bytes before being converted
     * to hexadecimal form.
     * <p>
     * This method exists purely for test traceability and debugging and
     * should not be confused with Fabric’s cryptographic block hashes.
     *
     * @return a deterministic hexadecimal string identifying this block
     */
    public String getHash() {
        return HexFormat.of()
                .formatHex(String.valueOf(transactions).getBytes(UTF_8));
    }
}
