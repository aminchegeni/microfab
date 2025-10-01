# Fabino 🧪⚡️

*A lightweight testing framework for Hyperledger Fabric, powered by JUnit 5, Testcontainers, and Microfab.*

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)  
[![Java](https://img.shields.io/badge/Java-17%2B-green.svg)]()  
[![JUnit5](https://img.shields.io/badge/Tested_with-JUnit_5-purple.svg)]()  
[![Docker](https://img.shields.io/badge/Docker-Microfab-orange.svg)]()

---

## ✨ What is Fabino?

**Fabino** is a Java testing library designed to make writing **unit and integration tests for Hyperledger Fabric
chaincode** easy, fast, and reproducible.

It combines:

- **JUnit 5 extensions** → Seamless integration with your test lifecycle.
- **Testcontainers** → Automatically start and manage a Microfab Docker container.
- **Microfab** → Bootstrap a full Hyperledger Fabric network using a simple JSON/YAML config.

### Why Fabino?

Writing tests for Hyperledger Fabric chaincode is usually complex: you need a running network, multiple organizations,
channels, and properly installed chaincode.

Fabino abstracts all of this:

- Spin up a Fabric network **on-demand in your tests**.
- Install, approve, and commit chaincodes automatically.
- Inject a ready-to-use **Fabric Gateway SDK instance** into your test class.
- Focus on writing **real test scenarios** for your chaincode logic.

---

## 🛠 How Fabino Works

1. **Configuration**  
   Provide
   a [Microfab configuration](https://github.com/hyperledger-labs/microfab/blob/main/docs/ConfiguringMicrofab.md) file
   describing your orgs, peers, orderers, and chaincodes.

2. **JUnit 5 Extension**  
   Annotate your test class
   with [@Microfab](src/main/java/ir/co/isc/spbp/blockchain/microfab/Microfab.java).

3. **Chaincode Lifecycle**  
   Fabino handles the full chaincode lifecycle automatically:
    - Package → Install → Approve → Commit

4. **Dependency Injection**  
   Any field annotated
   with [@Msp(org="...")](src/main/java/ir/co/isc/spbp/blockchain/microfab/Msp.java)
   gets injected with a **Fabric Gateway SDK** instance, ready to use.

---

## 🚀 Quick Example

```java
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
    private Gateway gateway; // injected by Fabino 🎉

    @Test
    void test() throws GatewayException {
        Network network = gateway.getNetwork("messaging");
        Contract contract = network.getContract("hello");
        byte[] result = contract.evaluateTransaction("say", "amin");
        Assertions.assertEquals("{\"name\":\"amin\"}", new String(result, UTF_8));
    }
}
