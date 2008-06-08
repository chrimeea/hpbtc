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
    private File file;
    
    public BTFile(File rootFolder, String path, int length) {
        this.path = path;
        this.length = length;
        this.rootFolder = rootFolder;
        file = new File(rootFolder + File.separator + path);
        file.mkdirs();
    }

    public File getFile() {
        return file;
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
