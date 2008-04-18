/*
 * Created on Jan 26, 2006
 *
 */
package hpbtc.client.selection.peer;

import hpbtc.client.comparator.DownloadComparator;
import hpbtc.client.comparator.TotalPiecesComparator;
import hpbtc.client.DownloadItem;
import hpbtc.client.peer.LightPeer;
import hpbtc.client.piece.LightPiece;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class DistributedStrategy extends PeerSelectionStrategy {

    static final long serialVersionUID = -6393794391268207229L;

    private static Logger logger = Logger.getLogger(DistributedStrategy.class.getName());

    /* (non-Javadoc)
     * @see hpbtc.selection.peer.SelectionStrategy#select(hpbtc.download.Piece)
     */
    @Override
    public List<LightPeer> select(LightPiece lp) {
        List<LightPeer> prs = lp.getPeers();
        if (prs.isEmpty()) {
            logger.fine("Peer list is empty in DistributedStrategy");
            return prs;
        }
        Collections.shuffle(prs);
        Collections.sort(prs, new DownloadComparator(false));
        int i = DownloadItem.PIPELINE_SIZE;
        if (prs.size() < i) {
            i = prs.size();
        }
        prs = prs.subList(0, i);
        LightPeer l = Collections.min(prs, new TotalPiecesComparator());
        logger.fine("Selected peer " + l.getPeer().getIp() + " for piece " + (lp.getPiece().getIndex() + 1) + " using DistributedStrategy");
        return Collections.singletonList(l);
    }
}
