package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

/**
 * World state implementation representing a CouchDB-backed ledger state.
 * <p>
 * This class exists to model the behavioral characteristics of CouchDB
 * within the mock ledger environment, most notably the ability to support
 * rich queries. All core state management logic is inherited from
 * {@link AbstractInMemWorldState}.
 * <p>
 * No actual CouchDB instance is used; this class serves solely as a
 * semantic marker that allows higher-level components to adapt behavior
 * based on the world state type.
 *
 * @see AbstractInMemWorldState
 * @see Type#COUCHDB
 */
public class Couchdb extends AbstractInMemWorldState {

    /**
     * Returns the world state type represented by this implementation.
     *
     * @return {@link Type#COUCHDB}
     */
    @Override
    public Type getType() {
        return Type.COUCHDB;
    }
}
