/*
 * Created on Jan 25, 2006
 *
 */
package hpbtc.util;

/**
 * @author chris
 *
 */
public class ComparatorUtil {

    /**
     * @param o1
     * @param o2
     * @return
     */
    public static int compare(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            return -1;
        } else if (o2 == null) {
            return 1;
        }
        return 0;
    }
    
    /**
     * @param b1
     * @param b2
     * @return
     */
    public static int compare(boolean b1, boolean b2) {
        int r;
        if (b1) {
            if (b2) {
                r = 0;
            } else {
                r = 1;
            }
        } else {
            r = -1;
        }
        return r;
    }
}
