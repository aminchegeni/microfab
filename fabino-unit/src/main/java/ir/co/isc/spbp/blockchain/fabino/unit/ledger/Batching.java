package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

import lombok.Value;

import java.time.Duration;

/**
 * Defines batching rules for grouping transactions into a block.
 * <p>
 * A block is closed and committed when either:
 * <ul>
 *   <li>The number of accumulated transactions reaches {@link #size}, or</li>
 *   <li>The configured {@link #timeout} elapses since the first transaction
 *       entered the batch.</li>
 * </ul>
 * <p>
 * This model simulates the behavior of a Fabric ordering service, where
 * transactions are buffered and cut into blocks based on size and time
 * thresholds rather than immediate submission.
 *
 * @see LedgerFacade
 */
@Value(staticConstructor = "of")
public class Batching {

    /**
     * Maximum number of transactions allowed in a single block.
     */
    int size;

    /**
     * Maximum time to wait before closing a block, regardless of its size.
     */
    Duration timeout;
}
