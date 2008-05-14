package hpbtc.processor;

import hpbtc.protocol.torrent.BTFile;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class PieceRepository {

    private static Logger logger = Logger.getLogger(PieceRepository.class.getName());
    public static final int DEFAULT_CHUNK = 16384;
    
    private List<BTFile> files;
    
    public PieceRepository(List<BTFile> files) {
        this.files = files;
    }
    
    private boolean checkHash(ByteBuffer bb, byte[] hash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            bb.rewind();
            md.update(bb);
            return Arrays.equals(md.digest(), hash);
        } catch (NoSuchAlgorithmException e) {
            logger.severe("SHA1 is not available");
        }
        return false;
    }
    
    /**
     * @param begin
     * @param length
     * @return
     * @throws IOException
     */
    public ByteBuffer getPiece(int begin, int length) throws IOException {
        return null;
    }

    /**
     * @param o
     * @param b
     * @return
     */
    public void savePiece(int begin, ByteBuffer b) {
    }
    
    private void flush() {
    }
    
    private void createFile(String path) throws IOException {
        File f = new File(path);
        f.mkdirs();
        f.createNewFile();
    }
}
