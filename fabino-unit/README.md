# Hyperledger Fabric Chaincode Unit Testing Infrastructure

This document describes a lightweight, in-memory testing infrastructure for Hyperledger Fabric chaincode, designed for use with **JUnit 5**.  
The goal is to simulate Fabric execution semantics (ledger, ordering, transactions, and stubs) **without running a Fabric network**.

The system is deterministic, cryptographically correct, and intentionally boring — which is exactly what test infrastructure should be.

---

## 1. StubInvocationContext

`StubInvocationContext` represents all metadata required to construct a Fabric-style chaincode invocation.

It acts as an immutable snapshot of a transaction invocation and is typically derived from the `@Stub` annotation.

### Responsibilities

- Hold client identity (MSP ID, certificate, private key)
- Define channel and chaincode identity
- Provide function arguments and transient data
- Resolve timestamps and nonces
- Normalize defaults into runtime-safe values

### Notable Behavior

- **Timestamp**
    - If the default timestamp is used, the current system time is substituted.
    - Converted into a Fabric `Timestamp` (`seconds + nanos`).

- **Nonce**
    - If a zero-filled nonce is provided, a secure random nonce is generated.
    - Nonces are always normalized to a fixed length (24 bytes).

- **Arguments**
    - The chaincode function name is prepended to the argument list automatically.

### Usage

`StubInvocationContext` is created internally via:

```java

StubInvocationContext.of(@Stub annotation)
