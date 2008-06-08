package hpbtc.protocol.torrent;

import java.io.File;

/**
 * @author chris
 *
 */
public class BTFile {
    
    private String path;
    private int length;
    private File rootFolder;
    
    public BTFile(File rootFolder, String path, int length) {
        this.path = path;
        this.length = length;
        this.rootFolder = rootFolder;
    }

    public File getRootFolder() {
        return rootFolder;
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
    public int getLength() {
        return length;
    }
}
