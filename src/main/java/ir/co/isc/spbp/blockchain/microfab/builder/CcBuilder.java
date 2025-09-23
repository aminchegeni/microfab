package ir.co.isc.spbp.blockchain.microfab.builder;

import ir.co.isc.spbp.blockchain.microfab.MicrofabContainer;
import ir.co.isc.spbp.blockchain.microfab.model.Config;

import static ir.co.isc.spbp.blockchain.microfab.model.Config.Chaincode;
import static ir.co.isc.spbp.blockchain.microfab.model.Config.Channel;

/**
 * Abstraction for building and running chaincodes in a {@link MicrofabContainer}.
 * <p>
 * This interface defines the lifecycle hooks required to support different
 * types of chaincodes (e.g., external CCaaS, in-process, or future chaincode models).
 * By delegating deployment logic to implementations of this interface,
 * the library can be extended with support for new chaincode packaging
 * and runtime strategies without changing the core container logic.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li><b>Detection:</b> Identify whether a given {@link Config.Chaincode}
 *   is supported by this builder.</li>
 *   <li><b>Build:</b> Prepare the chaincode for deployment to the specified
 *   {@link Channel}. This typically includes packaging, installing, and approving
 *   the chaincode definition.</li>
 *   <li><b>Run:</b> Start the chaincode execution environment inside or alongside
 *   the {@link MicrofabContainer}, depending on the implementation.</li>
 * </ul>
 *
 * <p>
 * Implementations are intended to encapsulate all the steps required for a
 * specific chaincode deployment model, giving flexibility to extend this
 * library with new chaincode types in the future.
 * </p>
 */
public interface CcBuilder {

    /**
     * Determines whether this builder can handle the given chaincode definition.
     *
     * @param chaincode the chaincode configuration from {@link Config}.
     * @return {@code true} if the builder supports this chaincode type,
     *         {@code false} otherwise.
     */
    boolean detect(Config.Chaincode chaincode);

    /**
     * Prepares the specified chaincode for deployment on the given channel.
     * <p>
     * This step may include tasks such as packaging, installing, and
     * approving the chaincode definition, depending on the chaincode type.
     * </p>
     *
     * @param container the running {@link MicrofabContainer} instance.
     * @param channel   the target channel for the chaincode.
     * @param chaincode the chaincode definition from the configuration.
     */
    void build(MicrofabContainer<?> container, Channel channel, Chaincode chaincode);

    /**
     * Runs the specified chaincode in the context of the provided channel.
     * <p>
     * This step typically involves starting the chaincode process (for
     * external builders such as CCaaS) or activating it within the Fabric
     * runtime environment.
     * </p>
     *
     * @param container the running {@link MicrofabContainer} instance.
     * @param channel   the target channel for the chaincode.
     * @param chaincode the chaincode definition from the configuration.
     */
    void run(MicrofabContainer<?> container, Channel channel, Chaincode chaincode);
}
