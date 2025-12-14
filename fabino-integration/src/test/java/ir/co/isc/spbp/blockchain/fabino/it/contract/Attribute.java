package ir.co.isc.spbp.blockchain.fabino.it.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * Represents a custom metadata field of an artwork.
 * It may contain {@code descriptive} or {@code contextual} information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@DataType
public class Attribute {

    /**
     * The name of the metadata field.
     * Must be {@code lowercase alphanumeric}, may contain {@code hyphens} or {@code underscores}
     * and be between {@code 1} and {@code 20} characters.
     */
    @Property(schema = {
            "title", "Attribute Name",
            "pattern", "^[a-z0-9_-]*$",
            "minLength", "1",
            "maxLength", "20"
    })
    private String name;

    /**
     * The assigned value for this attribute.
     * May contain {@code any} character and must be between {@code 1} and {@code 100} characters.
     */
    @Property(schema = {
            "title", "Attribute Value",
            "pattern", "^.*{1,100}$"
    })
    private String value;
}
