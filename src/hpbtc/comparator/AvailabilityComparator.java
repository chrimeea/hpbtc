/*
 * Created on Jan 26, 2006
 *
 */
package hpbtc.comparator;

import hpbtc.piece.LightPiece;
import hpbtc.util.ComparatorUtil;

import java.util.Comparator;

/**
 * @author chris
 *
 */
public class AvailabilityComparator implements Comparator<LightPiece> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(T, T)
     */
    public int compare(LightPiece p1, LightPiece p2) {
        int r = ComparatorUtil.compare(p1, p2);
        if (r == 0 && p1 != null && p2 != null) {
            r = new Integer(p1.getAvailability()).compareTo(p2.getAvailability());
        }
        return r;
    }

}
