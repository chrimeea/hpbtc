/*
 * Created on 17.10.2008
 */

package hpbtc.dht;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class DHTUtil {

    public static byte[] divideByTwo(byte[] n) {
        int j = n.length;
        byte[] b = new byte[j];
        for (int i = 0; i < j; i++) {
            int rem = n[i] & 1 << 7;
            b[i] = (byte) (n[i] >>> 1 | rem);
        }
        return b;
    }
}
