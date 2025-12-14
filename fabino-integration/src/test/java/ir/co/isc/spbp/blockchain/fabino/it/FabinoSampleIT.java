package ir.co.isc.spbp.blockchain.fabino.it;

import com.google.gson.Gson;
import ir.co.isc.spbp.blockchain.fabino.it.contract.Artwork;
import ir.co.isc.spbp.blockchain.fabino.it.contract.Attribute;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.protos.gateway.ErrorDetail;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static ir.co.isc.spbp.blockchain.fabino.it.Microfab.Organization;
import static ir.co.isc.spbp.blockchain.fabino.it.Microfab.Channel;
import static ir.co.isc.spbp.blockchain.fabino.it.Microfab.Tls;
import static ir.co.isc.spbp.blockchain.fabino.it.Microfab.Chaincode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@Microfab(
        image = "ghcr.io/hyperledger-labs/microfab",
        domain = "localho.st",
        port = 8585,
        orderingOrganization = @Organization(name = "Orderer"),
        endorsingOrganizations = {
                @Organization(name = "Org1"),
                @Organization(name = "Org2")
        },
        channels = @Channel(
                name = "test",
                endorsingOrganizations = {"Org1", "Org2"},
                chaincodes = {"artino"}
        ),
        capabilityLevel = "V2_0",
        couchdb = false,
        certificateAuthorities = false,
        timeout = "90s",
        tls = @Tls(
//            enabled = true
        ),
        chaincodes = {
                @Chaincode(
                        name = "artino",
                        version = "0.1.0",
                        address = "127.0.0.1:9999"
                )
        })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FabinoSampleIT {

    private static final Gson GSON = new Gson();

    @Msp(org = "Org1")
    private Gateway org1;

    private Artwork artwork;

    @BeforeEach
    void init() {
        artwork = Artwork.builder()
                .id(1_234_567)
                .name("Mona Lisa")
                .category("painting")
                .attributes(new Attribute[]{
                        Attribute.of("artist", "Leonardo da Vinci"),
                        Attribute.of("year", "1503-1519"),
                })
                .owner("A1B2C3D4E5")
                .build();
    }

    @Test
    @Order(1)
    void query_artwork_when_ledger_in_initial_state_return_empty_array() {
        Network network = org1.getNetwork("test");
        Contract contract = network.getContract("artino");

        byte[] result = assertDoesNotThrow(() -> contract.evaluateTransaction("QueryBy", "painting"));

        assertNotNull(result);
        assertEquals("[]", new String(result, UTF_8));
    }

    @Test
    @Order(2)
    void create_artwork_when_all_property_valid_return_created_artwork(@Msp(org = "Org2") Gateway org2) {
        Network network = org2.getNetwork("test");
        Contract contract = network.getContract("artino");
        Artwork expected = artwork;

        byte[] result = assertDoesNotThrow(() -> contract.submitTransaction("CreateArtwork", GSON.toJson(expected)));
        Artwork actual = GSON.fromJson(new String(result, UTF_8), Artwork.class);

        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    @Order(3)
    void create_artwork_when_artwork_is_duplicate_throws_exception(@Msp(org = "Org2") Gateway org2) {
        Network network = org2.getNetwork("test");
        Contract contract = network.getContract("artino");
        Artwork expected = artwork;

        EndorseException thrown = assertThrows(EndorseException.class,
                () -> contract.submitTransaction("CreateArtwork", GSON.toJson(expected)));

        assertNotNull(thrown);
        List<ErrorDetail> details = thrown.getDetails();
        assertNotNull(details);
        assertEquals(1, details.size());
        ErrorDetail error = details.getFirst();
        assertEquals("chaincode response 500, artwork already exists", error.getMessage());
    }

    @ParameterizedTest
    @Order(4)
    @ValueSource(ints = {999_999, 10_000_000})
    void create_artwork_when_id_is_not_valid_throws_exception(int id, @Msp(org = "Org2") Gateway org2) {
        Network network = org2.getNetwork("test");
        Contract contract = network.getContract("artino");
        artwork.setId(id);
        Artwork expected = artwork;

        EndorseException thrown = assertThrows(EndorseException.class,
                () -> contract.submitTransaction("CreateArtwork", GSON.toJson(expected)));

        assertNotNull(thrown);
        List<ErrorDetail> details = thrown.getDetails();
        assertNotNull(details);
        assertEquals(1, details.size());
        ErrorDetail error = details.getFirst();
        String message = error.getMessage();
        assertNotNull(message);
        assertTrue(message.startsWith("chaincode response 500, Validation Errors::#/prop/id: %d is not".formatted(id)));
    }

    @Disabled("TODO: sometimes validation error message is empty")
    @ParameterizedTest
    @Order(5)
    @ValueSource(strings = {"Amin-Chegeni", "Amin", "Aminnnnnnnn Chegeniiiiiiii"})
    void create_artwork_when_name_is_not_valid_throws_exception(String name) {
        Network network = org1.getNetwork("test");
        Contract contract = network.getContract("artino");
        artwork.setName(name);
        Artwork expected = artwork;

        EndorseException thrown = assertThrows(EndorseException.class,
                () -> contract.submitTransaction("CreateArtwork", GSON.toJson(expected)));

        assertNotNull(thrown);
        List<ErrorDetail> details = thrown.getDetails();
        assertNotNull(details);
        assertEquals(1, details.size());
        ErrorDetail error = details.getFirst();
        String message = error.getMessage();
        assertNotNull(message);
        assertTrue(message.startsWith(
                "chaincode response 500, Validation Errors::#/prop/name: string [%s] does not match pattern"
                        .formatted(name)));
    }

    @Test
    @Order(6)
    void create_artwork_when_category_is_not_valid_throws_exception() {
        Network network = org1.getNetwork("test");
        Contract contract = network.getContract("artino");
        artwork.setCategory("invalid");
        Artwork expected = artwork;

        EndorseException thrown = assertThrows(EndorseException.class,
                () -> contract.submitTransaction("CreateArtwork", GSON.toJson(expected)));

        assertNotNull(thrown);
        List<ErrorDetail> details = thrown.getDetails();
        assertNotNull(details);
        assertEquals(1, details.size());
        ErrorDetail error = details.getFirst();
        String message = error.getMessage();
        assertNotNull(message);
        assertEquals(
                "chaincode response 500, Validation Errors::#/prop/category: invalid is not a valid enum value",
                message);
    }

    @Test
    @Order(13)
    void query_artwork_when_ledger_transacted_return_non_empty_array() {
        Network network = org1.getNetwork("test");
        Contract contract = network.getContract("artino");

        byte[] result = assertDoesNotThrow(() -> contract.evaluateTransaction("QueryBy", "painting"));

        Artwork[] artworks = GSON.fromJson(new String(result, UTF_8), Artwork[].class);

        assertNotNull(artworks);
        assertEquals(1, artworks.length);
    }
}
