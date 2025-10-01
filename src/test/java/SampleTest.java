import ir.co.isc.spbp.blockchain.microfab.Microfab;
import ir.co.isc.spbp.blockchain.microfab.Msp;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static ir.co.isc.spbp.blockchain.microfab.Microfab.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@Microfab(
        image = "hyperledger-labs/microfab",
        domain = "example.com",
        port = 8585,
        orderingOrganization = @Organization(name = "Orderer"),
        endorsingOrganizations = {
                @Organization(name = "Org1"),
                @Organization(name = "Org2"),
                @Organization(name = "Org3")
        },
        channels = @Channel(
                name = "messaging",
                endorsingOrganizations = {"Org1", "Org2", "Org3"},
                chaincodes = {"hello"}
        ),
        capabilityLevel = "V2_0",
        couchdb = false,
        certificateAuthorities = false,
        tls = @Tls(
                enabled = true
        ),
        chaincodes = {
                @Chaincode(
                        name = "hello",
                        version = "1.0.0",
                        address = "127.0.0.1:9999"
                )
        })
class SampleTest {

    @Msp(org = "Org1")
    private Gateway gateway;

    @Test
    void test() throws GatewayException {
        Network network = gateway.getNetwork("messaging");
        Contract contract = network.getContract("hello");
        byte[] result = contract.evaluateTransaction("say", "amin");
        Assertions.assertEquals("{\"name\":\"amin\"}", new String(result, UTF_8));
    }
}
