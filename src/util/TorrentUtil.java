package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 *
 * @author chris
 */
public class TorrentUtil {

    public static int computeBeginIndex(int begin, int chunkSize) {
        return begin / chunkSize;
    }

    public static int computeEndIndex(int begin, int length, int chunkSize) {
        return 1 + (begin + length) / chunkSize;
    }

    public static int computeBeginPosition(int begin, int chunkSize) {
        return begin * chunkSize;
    }

    public static byte[] computeInfoHash(byte[] info)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        return md.digest(info);
    }
    
    public static byte[] generateId() {
        byte[] pid = new byte[20];
        pid[0] = 'C';
        pid[1] = 'M';
        pid[2] = '-';
        pid[3] = '2';
        pid[4] = '.';
        pid[5] = '0';
        Random r = new Random();
        r.nextBytes(pid);
        return pid;
    }
}
