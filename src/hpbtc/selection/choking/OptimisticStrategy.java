package hpbtc.selection.choking;

import hpbtc.peer.LightPeer;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class OptimisticStrategy extends ChokingStrategy {
    
    static final long serialVersionUID = -912644549913631660L;

    private static Logger logger = Logger.getLogger(OptimisticStrategy.class.getName());
    private Random r;
    
    /**
     * 
     */
    public OptimisticStrategy() {
        r = new Random();
    }

    /* (non-Javadoc)
     * @see hpbtc.selection.choking.ChokingStrategy#select(java.util.List)
     */
    public LightPeer select(List<LightPeer> lp) {
        LightPeer l;
        if (lp.isEmpty()) {
            l = null;
            logger.fine("Peer list is empty in OptimisticStrategy");
        } else {
            l = lp.get(r.nextInt(lp.size()));
            logger.fine("Selected peer " + l.getPeer().getIp() + " between " + lp.size() + " using OptimisticStrategy");
        }
        return l;
    }
}
