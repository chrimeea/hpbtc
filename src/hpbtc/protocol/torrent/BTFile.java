package hpbtc.protocol.torrent;

import java.io.File;

/**
 * @author Cristian Mocanu
 *
 */
public class BTFile {
    
    private String path;
    private long length;
    private File file;
    
    public BTFile(final String path, final long length) {
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
    public long getLength() {
        return length;
    }

    @Override
    public String toString() {
        return file.toString();
    }
}
