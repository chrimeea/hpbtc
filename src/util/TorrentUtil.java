package util;

import hpbtc.bencoding.BencodingWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 *
 * @author Administrator
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
    
    public static byte[] computeInfoHash(Map<String, Object> info)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BencodingWriter w = new BencodingWriter(os);
        w.write(info);
        md.update(os.toByteArray());
        return md.digest();
    }
}
