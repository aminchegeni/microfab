package ir.co.isc.spbp.blockchain.fabino.unit;

import ir.co.isc.spbp.blockchain.fabino.unit.ledger.Blockchain;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.InMemBlockchain;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.Leveldb;
import ir.co.isc.spbp.blockchain.fabino.unit.ledger.WorldState;
import lombok.Getter;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static ir.co.isc.spbp.blockchain.fabino.unit.Ledger.Scope.TEST;

/**
 * Declares and configures a logical ledger environment for tests.
 * <p>
 * This annotation is consumed by the test infrastructure to provision
 * a ledger composed of a {@link WorldState} and a {@link Blockchain}
 * implementation, with a configurable lifecycle {@link Scope}.
 * <p>
 * It may be applied at type, field, or method level to control how
 * ledger instances are created, shared, and isolated during test execution.
 */
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Ledger {

    /**
     * Defines the lifecycle scope of the ledger instance.
     * <p>
     * The scope controls instance reuse and visibility across tests.
     */
    Scope scope() default TEST;

    /**
     * World state implementation backing this ledger.
     * <p>
     * Determines how key-value state is stored and queried
     * during chaincode execution.
     */
    Class<? extends WorldState> worldState() default Leveldb.class;

    /**
     * Blockchain implementation used to record committed blocks.
     * <p>
     * Responsible for block creation, transaction ordering,
     * and block lifecycle simulation.
     */
    Class<? extends Blockchain> blockchain() default InMemBlockchain.class;

    /**
     * Initial seed entries applied to the ledger before test execution.
     * <p>
     * Each entry represents a preloaded state or bootstrap value
     * required for deterministic test setup.
     */
    String[] seeds() default {};

    /**
     * Defines the lifecycle scope for objects managed by the test infrastructure.
     *
     * <p>The scope determines <strong>how long a single instance is shared</strong>
     * and <strong>which test elements observe the same object</strong>.</p>
     *
     * <ul>
     *   <li>{@link #GLOBAL} – A single instance shared across the entire test engine
     *       execution.</li>
     *   <li>{@link #CLASS} – A single instance shared within one test class.</li>
     *   <li>{@link #TEST} – A new instance per test invocation.</li>
     * </ul>
     *
     * <p>Used by JUnit 5 extensions to select the appropriate
     * {@link org.junit.jupiter.api.extension.ExtensionContext.Store}.</p>
     */
    @Getter
    enum Scope {

        /**
         * One instance per test engine execution.
         */
        GLOBAL("global"),

        /**
         * One instance per test class.
         */
        CLASS("class"),

        /**
         * One instance per test method or iteration.
         */
        TEST("test");

        private final String name;

        Scope(String name) {
            this.name = name;
        }
    }
}
