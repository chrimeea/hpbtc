/*
 * Created on Jan 26, 2006
 *
 */
package hpbtc.client.selection.peer;

import hpbtc.client.peer.LightPeer;
import hpbtc.client.piece.LightPiece;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class EndGameStrategy extends PeerSelectionStrategy {

    static final long serialVersionUID = 8318352007758152337L;

    private static Logger logger = Logger.getLogger(EndGameStrategy.class.getName());

    /* (non-Javadoc)
     * @see hpbtc.selection.peer.SelectionStrategy#select(hpbtc.download.Piece)
     */
    @Override
    public List<LightPeer> select(LightPiece lp) {
        List<LightPeer> l = lp.getPeers();
        logger.fine("Selected " + l.size() + " peers for piece " + (lp.getPiece().getIndex() + 1) + " using EndGameStrategy");
        return l;
    }
}
