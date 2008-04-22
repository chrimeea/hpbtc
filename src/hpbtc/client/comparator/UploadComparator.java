/*
 * Created on Jan 25, 2006
 *
 */
package hpbtc.client.comparator;

import hpbtc.client.LightPeer;
import hpbtc.util.ComparatorUtil;

import java.util.Comparator;

/**
 * @author chris
 *
 */
public class UploadComparator implements Comparator<LightPeer> {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(T, T)
     */
    public int compare(LightPeer p1, LightPeer p2) {
        int r = ComparatorUtil.compare(p1, p2);
        if (r == 0 && p1 != null && p2 != null) {
            r = new Integer(p1.getUploadRate()).compareTo(p2.getUploadRate());
            if (r == 0) {
                r = ComparatorUtil.compare(!p1.isChokedHere(), !p2.isChokedHere());
            }
        }
        return r;
    }

}
