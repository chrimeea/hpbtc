package hpbtc.client.selection.choking;

import hpbtc.client.comparator.DownloadComparator;
import hpbtc.client.peer.LightPeer;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class DownloadStrategy extends ChokingStrategy {

    static final long serialVersionUID = 5334284324697529443L;

    private static Logger logger = Logger.getLogger(DownloadStrategy.class.getName());
    
    /* (non-Javadoc)
     * @see hpbtc.selection.choking.ChokingStrategy#select(java.util.List)
     */
    public LightPeer select(List<LightPeer> lp) {
        LightPeer l;
        if (lp.isEmpty()) {
            logger.fine("Peer list is empty in DownloadStrategy");
            l = null;
        } else {
            l = Collections.max(lp, new DownloadComparator(true));
            if (!l.isChoked()) {
                logger.fine("Peer list contains no unchoked peers in DownloadStrategy");
                l = null;
            } else {
                logger.fine("Selected peer " + l.getPeer().getIp() + " between " + lp.size() + " peers using DownloadStrategy");
            }
        }
        return l;
    }
}
