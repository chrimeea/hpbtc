package hpbtc.client.selection.choking;

import hpbtc.client.comparator.UploadComparator;
import hpbtc.client.LightPeer;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class UploadStrategy extends ChokingStrategy {

    static final long serialVersionUID = 7684866726409566829L;

    private static Logger logger = Logger.getLogger(UploadStrategy.class.getName());
    
    /* (non-Javadoc)
     * @see hpbtc.selection.choking.ChokingStrategy#select(java.util.List)
     */
    public LightPeer select(List<LightPeer> lp) {
        LightPeer l;
        if (lp.isEmpty()) {
            l = null;
            logger.fine("Peer list is empty in UploadStrategy");
        } else {
            l = Collections.max(lp, new UploadComparator());
            if (!l.isChoked()) {
                logger.fine("Peer list contains no unchoked peers in UploadStrategy");
                l = null;
            } else {
                logger.fine("Selected peer " + l.getPeer().getIp() + " between " + lp.size() + " using UploadStrategy");
            }
        }
        return l;
    }
}
