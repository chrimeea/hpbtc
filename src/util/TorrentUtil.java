package util;

/**
 *
 * @author Administrator
 */
public class TorrentUtil {

    public static int computeBeginPosition(int begin, int chunkSize) {
        return begin / chunkSize;
    }
    
    public static int computeEndPosition(int begin, int length, int chunkSize) {
        return 1 + (begin + length) / chunkSize;
    }
}
