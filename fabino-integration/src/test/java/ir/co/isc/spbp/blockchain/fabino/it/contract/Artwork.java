package ir.co.isc.spbp.blockchain.fabino.it.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * Represents a digital or physical artistic creation stored on-chain.
 * It contains metadata such as {@code name}, {@code owner}, {@code category},
 * and custom-defined {@code attributes}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DataType
public class Artwork {

    /**
     * Unique numeric identifier assigned to the artwork.
     * Must be a {@code 7-digit} number.
     */
    @Property(schema = {
            "title", "Artwork Identifier",
            "minimum", "1000000",
            "maximum", "9999999"
    })
    private int id;

    /**
     * The official registered name of the artwork.
     * Must be {@code alphanumeric}, allow {@code spaces}, and be between {@code 5} and {@code 25} characters.
     */
    @Property(schema = {
            "title", "Artwork Name",
            "pattern", "^[a-zA-Z0-9\\s]{5,25}$"
    })
    private String name;

    /**
     * Classification of the artwork. Only {@code predefined} values are allowed.
     */
    @Property(schema = {
            "title", "Artwork Category",
            "enum", "painting,video,music,photography,animation"
    })
    private String category;

    /**
     * Extended metadata tags for the artwork.
     * Must contain at least {@code two} entries.
     */
    @Property(schema = {
            "title", "Artwork Attributes",
            "uniqueItems", "true",
            "minItems", "2"
    })
    private Attribute[] attributes;

    /**
     * The identifier of the owner (wallet, user, or organization).
     * Must be exactly {@code 10 alphanumeric} characters, {@code uppercase} only.
     */
    @Property(schema = {
            "title", "Owner Identifier",
            "pattern", "^[A-Z0-9]*$",
            "minLength", "10",
            "maxLength", "10"
    })
    private String owner;
}
