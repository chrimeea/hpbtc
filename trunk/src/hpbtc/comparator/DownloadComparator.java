/*
 * Created on Jan 25, 2006
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
public class DownloadComparator implements Comparator<LightPeer> {
    
    private boolean ext;
    
    /**
     * @param e
     */
    public DownloadComparator(boolean e) {
        ext = e;
    }
    
    /* (non-Javadoc)
     * @see java.util.Comparator#compare(T, T)
     */
    public int compare(LightPeer p1, LightPeer p2) {
        int r = ComparatorUtil.compare(p1, p2);
        if (r == 0 && p1 != null && p2 != null) {
            if (ext) {
                r = ComparatorUtil.compare(!p1.isChoked(), !p2.isChoked());
                if (r == 0 && !p1.isChoked() && !p2.isChoked()) {
                    r = ComparatorUtil.compare(!p1.isSnubbed(), !p2.isSnubbed());
                    if (r == 0 && !p1.isSnubbed() && !p2.isSnubbed()) {
                        r = ComparatorUtil.compare(p1.isFree(), p2.isFree());
                        if (r == 0 && p1.isFree() && p2.isFree()) {
                            r = new Integer(p1.getDownloadRate()).compareTo(p2.getDownloadRate());
                            if (r == 0) {
                                r = ComparatorUtil.compare(!p1.isChokedHere(), !p2.isChokedHere());
                            }
                        }
                    }
                }
            } else {
                r = ComparatorUtil.compare(!p1.isSnubbed(), !p2.isSnubbed());
                if (r == 0 && !p1.isSnubbed() && !p2.isSnubbed()) {
                    r = ComparatorUtil.compare(p1.isFree(), p2.isFree());
                    if (r == 0 && p1.isFree() && p2.isFree()) {
                        r = new Integer(p1.getDownloadRate()).compareTo(p2.getDownloadRate());
                    }
                }
            }
        }
        return r;
    }

}
