package ir.co.isc.spbp.blockchain.microfab.test;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;

import static org.hyperledger.fabric.contract.annotation.Transaction.TYPE.EVALUATE;

/**
 * First Internal CC Ver 1.0
 */
@Default
@Contract(name = "Hello")
public class HelloContract implements ContractInterface {

    @Transaction(intent = EVALUATE)
    public Hello say(Context ctx, String name) {
        return new Hello(name);
    }
}
