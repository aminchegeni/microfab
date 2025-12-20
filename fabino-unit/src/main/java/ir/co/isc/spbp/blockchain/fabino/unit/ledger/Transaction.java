package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

import lombok.Value;
import org.hyperledger.fabric.shim.ledger.KeyModification;

import java.time.Instant;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents a simplified, immutable transaction record applied to a single
 * world-state key within a mock Hyperledger Fabric ledger.
 * <p>
 * In Hyperledger Fabric, transactions are grouped into blocks and appended
 * to the blockchain. Each transaction may modify one or more keys in the
 * world state. When querying key history via the Fabric chaincode API,
 * results are exposed as a sequence of {@link KeyModification} entries.
 * <p>
 * This class models a <strong>single key-level modification</strong> within
 * a transaction and is designed to be aggregated into a block structure
 * managed by a mock {@code Blockchain} implementation.
 *
 * <h2>Role in the Mock Ledger Architecture</h2>
 * <ul>
 *   <li>Each {@code Transaction} represents one logical write or delete
 *       operation on a specific key.</li>
 *   <li>Multiple {@code Transaction} instances are aggregated into a
 *       {@code Block}.</li>
 *   <li>Blocks are appended to a {@code Blockchain} in commit order.</li>
 *   <li>The {@code WorldState} reflects the latest committed transaction
 *       per key.</li>
 * </ul>
 *
 * <p>
 * By implementing {@link KeyModification}, this class can be returned
 * directly from mock implementations of Fabric history queries such as
 * {@code getHistoryForKey}, allowing chaincode logic to be tested without
 * modification.
 *
 * <h2>Simplifications Compared to Real Fabric Transactions</h2>
 * <ul>
 *   <li>No read-set or write-set separation.</li>
 *   <li>No endorsement, validation, or MVCC version checks.</li>
 *   <li>No channel, namespace, or block metadata.</li>
 *   <li>Each instance is assumed to be committed and final.</li>
 * </ul>
 *
 * <p>
 * The class is immutable and thread-safe by design, making it suitable for
 * deterministic and repeatable tests—even under enthusiastic parallel execution.
 *
 * @see org.hyperledger.fabric.shim.ledger.KeyModification
 */
@Value
public class Transaction implements KeyModification {

    /**
     * The world-state key affected by this transaction.
     */
    String key;

    /**
     * The unique transaction identifier.
     * <p>
     * This value corresponds to the Fabric transaction ID ({@code TxId})
     * and is used when exposing transaction history to chaincode.
     */
    String id;

    /**
     * The value written for the key.
     * <p>
     * A {@code null} or empty value may be present when {@link #deleted}
     * is {@code true}.
     */
    byte[] value;

    /**
     * The commit timestamp of the transaction.
     * <p>
     * This timestamp represents when the transaction was committed to
     * the blockchain, not when it was proposed or endorsed.
     */
    Instant timestamp;

    /**
     * Indicates whether this transaction represents a delete operation.
     * <p>
     * When {@code true}, the key is considered removed from the world state
     * as of this transaction.
     */
    boolean deleted;

    /**
     * Returns the transaction identifier.
     *
     * @return the transaction ID
     */
    @Override
    public String getTxId() {
        return this.id;
    }

    /**
     * Returns the raw byte value written by this transaction.
     *
     * @return the value as a byte array, or {@code null} if deleted
     */
    @Override
    public byte[] getValue() {
        return this.value;
    }

    /**
     * Returns the value written by this transaction as a UTF-8 string.
     * <p>
     * This mirrors the behavior of Fabric’s {@code getStringValue()} helper
     * and is provided for convenience in tests.
     *
     * @return the value decoded as a UTF-8 string
     */
    @Override
    public String getStringValue() {
        return new String(this.value, UTF_8);
    }

    /**
     * Returns the commit timestamp of the transaction.
     *
     * @return the transaction timestamp
     */
    @Override
    public Instant getTimestamp() {
        return this.timestamp;
    }

    /**
     * Indicates whether this transaction deleted the key.
     *
     * @return {@code true} if the key was deleted, otherwise {@code false}
     */
    @Override
    public boolean isDeleted() {
        return this.deleted;
    }
}
