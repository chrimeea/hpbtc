package hpbtc.protocol.torrent;

import java.nio.ByteBuffer;
import java.util.List;

/**
 *
 * @author Cristian Mocanu
 */
public class FileStore {
    
    public FileStore() {
    }
    
    public ByteBuffer loadFileChunk(List<BTFile> files, int begin, int length) {
        return null;
    }
    
    public void saveFileChunk(List<BTFile> files, int begin, ByteBuffer piece) {
    }
}
