package hpbtc.bencoding;

import java.util.Comparator;

/**
 *
 * @author Cristian Mocanu
 */
public class ByteStringComparator implements Comparator<byte[]> {

    public int compare(byte[] b1, byte[] b2) {
        final int j = Math.min(b1.length, b2.length);
        for (int i = 0; i < j; i++) {
            if (b1[i] != b2[i]) {
                return b1[i] - b2[i];
            }
        }
        if (b1.length != b2.length) {
            return b1.length - b2.length;
        }
        return 0;
    }
}
