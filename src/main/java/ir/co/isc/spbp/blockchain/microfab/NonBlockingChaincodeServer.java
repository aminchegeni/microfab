package ir.co.isc.spbp.blockchain.microfab;

import org.hyperledger.fabric.shim.*;

import java.io.IOException;

public class NonBlockingChaincodeServer implements ChaincodeServer {

    /** Server. */
    private final GrpcServer grpcServer;

    /**
     * configure and init server.
     *
     * @param chaincodeBase - chaincode implementation (invoke, init)
     * @param chaincodeServerProperties - setting for grpc server
     */
    public NonBlockingChaincodeServer(
            final ChaincodeBase chaincodeBase, final ChaincodeServerProperties chaincodeServerProperties)
            throws IOException {
        // create listener and grpc server
        grpcServer = new NettyGrpcServer(chaincodeBase, chaincodeServerProperties);
    }

    /**
     * run external chaincode server.
     *
     * @throws IOException problem while start grpc server
     */
    @Override
    public void start() throws IOException {
        grpcServer.start();
    }

    /** shutdown now grpc server. */
    @Override
    public void stop() {
        grpcServer.stop();
    }
}
