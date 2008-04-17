package hpbtc.client.selection.piece;

import hpbtc.client.piece.LightPiece;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class RandomStrategy extends PieceSelectionStrategy {

    static final long serialVersionUID = -3776220527432252948L;

    private static Logger logger = Logger.getLogger(RandomStrategy.class.getName());
    
    /* (non-Javadoc)
     * @see hpbtc.selection.piece.SelectionStrategy#select(java.util.List)
     */
    protected LightPiece select(List<LightPiece> rem) {
        LightPiece lp = rem.get(0);
        logger.fine("Selected piece " + (lp.getPiece().getIndex() + 1) + " between " + rem.size() + " pieces using RandomStrategy");
        return lp;
    }
}
