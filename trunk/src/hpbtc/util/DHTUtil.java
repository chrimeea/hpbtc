/*
 * Created on 17.10.2008
 */

package hpbtc.util;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class DHTUtil {

    public static byte[] divideByTwo(final byte[] n) {
        int j = n.length;
        byte[] b = new byte[j];
        for (int i = 0; i < j; i++) {
            int rem = n[i] & 1 << 7;
            b[i] = (byte) (n[i] >>> 1 | rem);
        }
        return b;
    }

    public static byte[] computeDistance(final byte[] n1, final byte[] n2) {
        final byte[] n = new byte[20];
        for (int i = 0; i < 20; i++) {
            n[i] = (byte) (n1[i] ^ n2[i]);
        }
        return n;
    }
}
