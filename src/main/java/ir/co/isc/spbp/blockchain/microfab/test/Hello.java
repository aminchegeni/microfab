package ir.co.isc.spbp.blockchain.microfab.test;

import lombok.Data;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;

@Data
@DataType
public class Hello {

    @Property
    private String name;

    public Hello() {
    }

    public Hello(String name) {
        this.name = name;
    }

    public String toJSONString() {
        return new JSONObject(this).toString();
    }

    public static Hello fromJSONString(String json) {
        String name = new JSONObject(json).getString("name");
        Hello asset = new Hello();
        asset.setName(name);
        return asset;
    }
}
