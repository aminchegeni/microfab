package ir.co.isc.spbp.blockchain.fabino.contract;

import com.google.gson.Gson;
import ir.co.isc.spbp.blockchain.fabino.Microfab;
import ir.co.isc.spbp.blockchain.fabino.Msp;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.Network;
import org.junit.jupiter.api.*;

import static ir.co.isc.spbp.blockchain.fabino.Microfab.Organization;
import static ir.co.isc.spbp.blockchain.fabino.Microfab.Channel;
import static ir.co.isc.spbp.blockchain.fabino.Microfab.Tls;
import static ir.co.isc.spbp.blockchain.fabino.Microfab.Chaincode;
import static ir.co.isc.spbp.blockchain.fabino.utils.Util.toUnchecked;
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
class FabinoSampleTest {

    private static final Gson GSON = new Gson();

    @Msp(org = "Org1")
    private Gateway org1;

    @Test
    @Order(1)
    void query_artwork_when_ledger_in_initial_state_return_empty_array() {
        Network network = org1.getNetwork("test");
        Contract contract = network.getContract("artino");

        byte[] result = toUnchecked(() -> contract.evaluateTransaction("QueryBy", "painting"));

        assertEquals("[]", new String(result, UTF_8));
    }

    @Test
    @Order(2)
    void create_artwork_when_all_property_valid_return_created_artwork(@Msp(org = "Org2") Gateway org2) {
        Network network = org2.getNetwork("test");
        Contract contract = network.getContract("artino");
        Artwork expected = Artwork.builder()
                .id(1234567)
                .name("Mona Lisa")
                .category("painting")
                .attributes(new Attribute[]{
                        Attribute.of("artist", "Leonardo da Vinci"),
                        Attribute.of("year", "1503-1519"),
                })
                .owner("A1B2C3D4E5")
                .build();

        byte[] result = toUnchecked(() -> contract.submitTransaction("CreateArtwork", GSON.toJson(expected)));
        Artwork actual = GSON.fromJson(new String(result, UTF_8), Artwork.class);

        assertEquals(expected, actual);
    }

    @Test
    @Order(3)
    void query_artwork_when_ledger_transacted_return_non_empty_array() {
        Network network = org1.getNetwork("test");
        Contract contract = network.getContract("artino");

        byte[] result = toUnchecked(() -> contract.evaluateTransaction("QueryBy", "painting"));

        Artwork[] artworks = GSON.fromJson(new String(result, UTF_8), Artwork[].class);

        assertNotNull(artworks);
        assertEquals(1, artworks.length);
    }
}
