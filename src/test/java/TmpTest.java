import ir.co.isc.spbp.blockchain.microfab.Microfab;
import ir.co.isc.spbp.blockchain.microfab.Msp;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.Network;
import org.junit.jupiter.api.Test;

@Microfab(
        orderingOrganization = @Microfab.Organization(name = "hamid-khan"),
        endorsingOrganizations = {
                @Microfab.Organization(name = "mat"),
                @Microfab.Organization(name = "mb"),
                @Microfab.Organization(name = "parizi")
        },
        channels = @Microfab.Channel(
                name = "tokenization",
                endorsingOrganizations = {"mat", "mb", "parizi"},
                chaincodes = {"hello"}
        ),
        couchdb = false,
        certificateAuthorities = false,
        timeout = "160s",
        tls = @Microfab.Tls(
                enabled = true
        ),
        chaincodes = {
                @Microfab.Chaincode(
                        name = "hello"
                )
        }
)
class TmpTest {

    @Msp(org = "mat")
    private Gateway gateway;

    @Test
    void test() throws GatewayException {
        Network network = gateway.getNetwork("tokenization");
        Contract contract = network.getContract("hello");
        byte[] result = contract.evaluateTransaction("say", "amin");
        System.out.println(new String(result));
    }
}
