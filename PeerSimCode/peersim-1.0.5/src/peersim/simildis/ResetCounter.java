package peersim.simildis;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;


public class ResetCounter implements Control {

	 /**
     * The protocol to operate on.
     * 
     * @config
     */
    private static final String PAR_PROT = "protocol";

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    /** Value obtained from config property {@link #PAR_PROT}. */
    private final int protocolID;

    // ------------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------------
    /**
     * Standard constructor that reads the configuration parameters. Invoked by
     * the simulation engine.
     * 
     * @param prefix
     *            the configuration prefix for this class.
     */
    public ResetCounter(String prefix) {
        protocolID = Configuration.getPid(prefix + "." + PAR_PROT);
    }
	
	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		for (int i = 0; i < Network.size(); i++) {
			((SimilDis) Network.get(i).getProtocol(protocolID)).counter = 0;
			System.out.println("Reset Node:"+ i);
			
		}
		return false;
	}

	@Override
	public boolean execute(int exp) {
		// TODO Auto-generated method stub
		return false;
	}

}
