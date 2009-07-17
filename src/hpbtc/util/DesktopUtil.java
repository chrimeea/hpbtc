/*
 * Created on 15.10.2008
 */

package hpbtc.util;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class DesktopUtil {
    
    public static String getETA(long total, float speedpersec) {
        if (speedpersec == 0) {
            return "inf";
        }
        float eta = total / speedpersec;
        if (eta < 60) {
            return String.format("%1$.1fs", eta);
        } else if (eta < 3600) {
            return String.format("%1$.1fm", eta / 60);
        } else if (eta < 86400) {
            return String.format("%1$.1fh", eta / 3600);
        } else if (eta < 604800) {
            return String.format("%1$.1fd", eta / 86400);
        } else {
            return String.format("%1$.1fw", eta / 604800);
        }
    }

    public static String getRepresentation(final int value) {
        if (value < 1024) {
            return String.format("%1$db/s", value);
        } else if (value < 1048576) {
            return String.format("%1$.1fKb/s", value / 1024f);
        } else {
            return String.format("%1$.1fMb/s", value / 1048576f);
        }
    }
}
