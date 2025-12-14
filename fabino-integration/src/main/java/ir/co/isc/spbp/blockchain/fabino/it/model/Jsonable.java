package ir.co.isc.spbp.blockchain.fabino.it.model;

import org.json.JSONObject;

public interface Jsonable {

    default String toJson() {
        return new JSONObject(this).toString(2);
    }
}
