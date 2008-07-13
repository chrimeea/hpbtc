package util;

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
}
