package ir.co.isc.spbp.blockchain.fabino.unit;

import com.google.protobuf.ByteString;
import lombok.experimental.UtilityClass;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.protos.common.ChannelHeader;
import org.hyperledger.fabric.protos.common.Header;
import org.hyperledger.fabric.protos.common.HeaderType;
import org.hyperledger.fabric.protos.common.SignatureHeader;
import org.hyperledger.fabric.protos.msp.SerializedIdentity;
import org.hyperledger.fabric.protos.peer.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hyperledger.fabric.client.Hash.SHA256;
import static ir.co.isc.spbp.blockchain.fabino.unit.StubInvocationContext.ChaincodeContext;

/**
 * Factory for constructing Fabric {@link ChaincodeMessage} instances.
 * <p>
 * This class builds a fully populated transaction message that closely mirrors
 * the structure produced by a real Fabric SDK, including:
 * <ul>
 *   <li>Serialized creator identity</li>
 *   <li>Deterministic transaction ID computation</li>
 *   <li>Signed proposal generation</li>
 *   <li>Chaincode invocation payload</li>
 * </ul>
 *
 * <p>
 * Intended for unit and integration testing where realistic Fabric messages are
 * required without running a peer or orderer.
 */
@UtilityClass
public class MessageFactory {

    /**
     * Creates a Fabric {@link ChaincodeMessage} of type {@code TRANSACTION}
     * from the given {@link StubInvocationContext}.
     * <p>
     * The resulting message includes:
     * <ul>
     *   <li>A {@link Proposal} with valid headers and payload</li>
     *   <li>A {@link SignedProposal} using the creator's private key</li>
     *   <li>A transaction ID derived from nonce and creator identity</li>
     * </ul>
     *
     * <p>
     * The message is suitable for submission to mocked peer or chaincode
     * execution pipelines.
     *
     * @param ctx invocation context defining identity, chaincode, arguments,
     *            timestamp, nonce, and transient data
     * @return a fully constructed {@link ChaincodeMessage}
     * @throws GeneralSecurityException if private key parsing or signing fails
     */
    public static ChaincodeMessage of(StubInvocationContext ctx) throws GeneralSecurityException {

        SerializedIdentity serializedIdentity = SerializedIdentity.newBuilder()
                .setMspid(ctx.getMspId())
                .setIdBytes(ByteString.copyFromUtf8(ctx.getCert()))
                .build();

        byte[] nonce = ctx.getNonce();
        String txId = txId(nonce, serializedIdentity.toByteArray());

        SignatureHeader signatureHeader = SignatureHeader.newBuilder()
                .setCreator(serializedIdentity.toByteString())
                .setNonce(ByteString.copyFrom(nonce))
                .build();

        ChaincodeContext chaincodeContext = ctx.getChaincode();
        ChaincodeID chaincodeId = ChaincodeID.newBuilder()
                .setName(chaincodeContext.getName())
                .setVersion(chaincodeContext.getVersion())
                .build();

        String channel = ctx.getChannel();
        ChannelHeader channelHeader = ChannelHeader.newBuilder()
                .setType(HeaderType.ENDORSER_TRANSACTION.getNumber())
                .setVersion(0)
                .setTimestamp(ctx.getTimestamp())
                .setChannelId(channel)
                .setTxId(txId)
                .setEpoch(0L)
                .setExtension(
                        ChaincodeHeaderExtension.newBuilder()
                                .setChaincodeId(chaincodeId)
                                .build()
                                .toByteString()
                )
                .setTlsCertHash(ByteString.EMPTY)
                .build();

        Header header = Header.newBuilder()
                .setChannelHeader(channelHeader.toByteString())
                .setSignatureHeader(signatureHeader.toByteString())
                .build();

        ChaincodeInput input = ChaincodeInput.newBuilder()
                .addAllArgs(
                        ctx.getArgs()
                                .stream()
                                .map(ByteString::copyFromUtf8)
                                .toList()
                )
                .build();

        ChaincodeProposalPayload proposalPayload = ChaincodeProposalPayload.newBuilder()
                .setInput(
                        ChaincodeInvocationSpec.newBuilder()
                                .setChaincodeSpec(ChaincodeSpec.newBuilder()
                                        .setType(ChaincodeSpec.Type.JAVA)
                                        .setChaincodeId(chaincodeId)
                                        .setInput(input)
                                        .build()
                                )
                                .build()
                                .toByteString()
                )
                .putAllTransientMap(
                        ctx.getTransients()
                                .entrySet()
                                .stream()
                                .collect(Collectors.toUnmodifiableMap(
                                        Map.Entry::getKey,
                                        e -> ByteString.copyFromUtf8(e.getValue())))
                )
                .build();

        Proposal proposal = Proposal.newBuilder()
                .setHeader(header.toByteString())
                .setPayload(proposalPayload.toByteString())
                .setExtension(ByteString.EMPTY)
                .build();

        byte[] digest = SHA256.apply(proposal.toByteArray());
        Signer signer = Signers.newPrivateKeySigner(Identities.readPrivateKey(ctx.getKey()));

        byte[] signature = signer.sign(digest);
        SignedProposal signedProposal = SignedProposal.newBuilder()
                .setProposalBytes(proposal.toByteString())
                .setSignature(ByteString.copyFrom(signature))
                .build();

        return ChaincodeMessage.newBuilder()
                .setType(ChaincodeMessage.Type.TRANSACTION)
                .setPayload(input.toByteString())
                .setTxid(txId)
                .setProposal(signedProposal)
                .setChannelId(channel)
                .build();
    }

    /**
     * Computes a Fabric-compatible transaction ID.
     * <p>
     * This method follows the same logic as Fabric's
     * {@code ComputeTxID(nonce, creator)} function:
     * <pre>
     * txId = SHA256(nonce || creator)
     * </pre>
     *
     * @param nonce   transaction nonce
     * @param creator serialized creator identity
     * @return hex-encoded transaction ID
     */
    // hyperledger/fabric/protoutil/proputils.go -> func ComputeTxID(nonce, creator []byte) string
    public static String txId(byte[] nonce, byte[] creator) {
        int len = nonce.length + creator.length;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(len)) {
            bos.write(nonce);
            bos.write(creator);
            return HexFormat.of().formatHex(SHA256.apply(bos.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
