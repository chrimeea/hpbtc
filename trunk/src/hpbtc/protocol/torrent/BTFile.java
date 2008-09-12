package hpbtc.protocol.torrent;

import java.io.File;

/**
 * @author Cristian Mocanu
 *
 */
public class BTFile {
    
    private String path;
    private int length;
    private File file;
    
    public BTFile(final String path, final int length) {
        this.path = path;
        this.length = length;
        file = new File(path);
        file.getParentFile().mkdirs();
    }

    public File getFile() {
        return file;
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
