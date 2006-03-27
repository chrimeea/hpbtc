package hpbtc.selection.piece;

import hpbtc.comparator.AvailabilityComparator;
import hpbtc.piece.LightPiece;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class RarestFirstStrategy extends PieceSelectionStrategy {

    static final long serialVersionUID = 4541854298971838711L;

    private static Logger logger = Logger.getLogger(RarestFirstStrategy.class.getName());
    
    /* (non-Javadoc)
     * @see hpbtc.selection.piece.SelectionStrategy#select(java.util.List)
     */
    protected LightPiece select(List<LightPiece> rem) {
        LightPiece lp = Collections.min(rem, new AvailabilityComparator());
        logger.fine("Selected piece " + (lp.getPiece().getIndex() + 1) + " between " + rem.size() + " pieces using RarestFirstStrategy");
        return lp;
    }
}
