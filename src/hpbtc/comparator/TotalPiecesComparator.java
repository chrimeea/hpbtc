/*
 * Created on Jan 27, 2006
 *
 */
package hpbtc.comparator;

import hpbtc.peer.LightPeer;
import hpbtc.util.ComparatorUtil;

import java.util.Comparator;

/**
 * @author chris
 *
 */
public class TotalPiecesComparator implements Comparator<LightPeer> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(T, T)
     */
    public int compare(LightPeer p0, LightPeer p1) {
        int r =ComparatorUtil.compare(p0, p1);
        if (r == 0 && p0 != null && p1 != null) {
            r = new Integer(p0.getTotalPieces()).compareTo(p1.getTotalPieces());
        }
        return r;
    }

}
