package hpbtc.client.selection.piece;

import hpbtc.client.DownloadItem;
import hpbtc.client.peer.LightPeer;
import hpbtc.client.piece.LightPiece;
import hpbtc.client.piece.Piece;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public abstract class PieceSelectionStrategy implements Serializable {
    
    private static Logger logger = Logger.getLogger(PieceSelectionStrategy.class.getName());
    
    /**
     * @param item
     * @return
     */
    public LightPiece select(DownloadItem item) {
        List<LightPiece> lp = getRemaining(item);
        if (lp.isEmpty()) {
            logger.fine("No pieces are suitable for download");
            return null;
        }
        LightPiece l = lp.get(0);
        Piece p = l.getPiece();
        if (p.isStarted()) {
            logger.fine("Selected piece " + (p.getIndex() + 1) + " to finish");
            return l;
        }
        Collections.shuffle(lp);
        return select(lp);
    }

    protected abstract LightPiece select(List<LightPiece> rem);

    protected List<LightPiece> getRemaining(DownloadItem item) {
        LinkedList<LightPiece> rem = new LinkedList<LightPiece>();
        for (int i = 0; i < item.getTotalPieces(); i++) {
            Piece p = item.getPiece(i);
            if (!p.willComplete()) {
                List<LightPeer> avail = p.getAvailablePeers();
                if (!avail.isEmpty()) {
                    LightPiece lp = new LightPiece(p);
                    lp.setPeers(avail);
                    if (p.isStarted()) {
                        rem.addFirst(lp);
                    } else {
                        rem.addLast(lp);
                    }
                }
            }
        }
        return rem;
    }
}
