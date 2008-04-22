package hpbtc.protocol.torrent;

/**
 * @author chris
 *
 */
public class BTFile {
    
    private String path;
    private long length;
    
    public BTFile(String path, long length) {
        this.path = path;
        this.length = length;
    }
        
    /**
     * @return
     */
    public String getPath() {
        return path;
    }
    
    /**
     * @return
     */
    public long getLength() {
        return length;
    }
}
