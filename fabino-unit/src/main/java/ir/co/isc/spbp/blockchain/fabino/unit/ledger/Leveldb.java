package ir.co.isc.spbp.blockchain.fabino.unit.ledger;

/**
 * World state implementation representing a LevelDB-backed ledger state.
 * <p>
 * This class acts as a semantic marker for a world state that does not
 * support rich queries. All state storage and query mechanics are
 * inherited from {@link AbstractInMemWorldState}.
 *
 * @see AbstractInMemWorldState
 * @see Type#LEVELDB
 */
public class Leveldb extends AbstractInMemWorldState {

    /**
     * Returns the world state type represented by this implementation.
     *
     * @return {@link Type#LEVELDB}
     */
    @Override
    public Type getType() {
        return Type.LEVELDB;
    }
}
