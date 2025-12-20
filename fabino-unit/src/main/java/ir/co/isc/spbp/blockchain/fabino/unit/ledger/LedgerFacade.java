package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

import ir.co.isc.spbp.blockchain.fabino.unit.Ledger;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Predicate.not;

/**
 * Facade coordinating {@link WorldState}, {@link Blockchain}, and batching logic.
 * <p>
 * This class simulates the behavior of a Hyperledger Fabric peer combined with
 * an orderer, including transaction submission, batching, block creation,
 * and state commitment.
 * <p>
 * It is designed for deterministic and isolated testing rather than performance
 * or full protocol fidelity.
 */
@Value(staticConstructor = "of")
@Getter(AccessLevel.NONE)
public class LedgerFacade implements AutoCloseable {

    /**
     * Holds the latest committed value for each key.
     * <p>
     * Reads always reflect committed state only.
     */
    @NonNull
    WorldState worldState;

    /**
     * Stores the immutable sequence of committed blocks.
     */
    @NonNull
    Blockchain blockchain;

    /**
     * Defines when a block is closed based on size or time.
     * <p>
     * Simulates orderer batching behavior.
     */
    Batching batching = Batching.of(10, Duration.ofSeconds(2L));

    /**
     * Scheduler responsible for time-based block commits.
     */
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Queue representing the ordering service input buffer.
     * <p>
     * Transactions preserve insertion order.
     */
    BlockingQueue<Transaction> orderer = new LinkedBlockingQueue<>(2 << 12);

    /**
     * Monotonically increasing block number.
     */
    AtomicInteger blockNumber = new AtomicInteger(0);

    {
        long timeout = batching.getTimeout().toMillis();
        scheduler.scheduleAtFixedRate(
                () -> commit(CommitType.TIME_BASED),
                0,
                timeout,
                MILLISECONDS
        );
    }

    /**
     * Creates a {@code LedgerFacade} from a {@link Ledger} annotation definition.
     * <p>
     * World state and blockchain implementations are instantiated reflectively
     * and seeded before use.
     */
    public static LedgerFacade of(Ledger ledger) {
        try {
            WorldState worldState =
                    ledger.worldState().getDeclaredConstructor().newInstance();
            Blockchain blockchain =
                    ledger.blockchain().getDeclaredConstructor().newInstance();

            LedgerFacade facade = of(worldState, blockchain);
            init(facade, ledger.seeds());
            return facade;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies seed entries as synthetic transactions committed in a single block.
     */
    private static void init(LedgerFacade ledger, String[] states) {
        if (states.length > 0) {
            Arrays.stream(states)
                    .distinct()
                    .filter(not(String::isBlank))
                    .map(line -> line.split(",", 2))
                    .filter(kv -> kv.length == 2)
                    .map(kv ->
                            new Transaction(
                                    kv[0],
                                    "F".repeat(64),
                                    kv[1].getBytes(UTF_8),
                                    Instant.now(),
                                    false
                            ))
                    .forEach(ledger::submit);

            ledger.commit(CommitType.TIME_BASED);
        }
    }

    /**
     * Returns the committed value for a given key.
     */
    public String getState(String key) {
        return worldState.get(key);
    }

    /**
     * Submits a transaction to the ordering queue.
     * <p>
     * A size-based commit may be triggered immediately.
     */
    public void submit(Transaction tx) {
        orderer.add(tx);
        if (orderer.size() >= batching.getSize()) {
            commit(CommitType.SIZE_BASED);
        }
    }

    /**
     * Queries committed state entries by key prefix.
     */
    public List<Map.Entry<String, String>> queryState(String keyPrefix) {
        return worldState.query(keyPrefix);
    }

    /**
     * Returns all committed blocks.
     */
    public List<Block> getBlocks() {
        return blockchain.getBlocks();
    }

    /**
     * Returns a block by index, if present.
     */
    public Optional<Block> getBlock(int idx) {
        return blockchain.getBlock(idx);
    }

    /**
     * Shuts down background scheduling gracefully.
     */
    @Override
    public void close() {
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Commits transactions into a new block.
     * <p>
     * Depending on the commit type, transactions are drained either fully
     * (time-based) or up to the configured batch size (size-based).
     */
    private synchronized void commit(CommitType type) {

        Set<Transaction> uniques;
        if (CommitType.TIME_BASED == type) {
            uniques = new LinkedHashSet<>(orderer.size(), 1.0F);
            orderer.drainTo(uniques);
        } else {
            int batchSize = batching.getSize();
            if (orderer.size() >= batchSize) {
                uniques = new LinkedHashSet<>(batchSize, 1.0F);
                orderer.drainTo(uniques, batchSize);
            } else {
                return;
            }
        }

        if (uniques.isEmpty()) {
            return;
        }

        uniques.forEach(tx -> {
            if (tx.isDeleted()) {
                worldState.delete(tx.getKey());
            } else {
                worldState.put(tx.getKey(), tx.getStringValue());
            }
        });

        blockchain.append(new Block(blockNumber.getAndIncrement(), List.copyOf(uniques)));
    }

    /**
     * Commit trigger source.
     */
    private enum CommitType {
        TIME_BASED,
        SIZE_BASED
    }
}
