package hpbtc.download;

import hpbtc.Client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author chris
 *
 */
public class BTFile {
    
    private String path;
    private int length;
    private int pieceIndex;
    private int index;
    private int offset;

    public boolean create() throws IOException {
        int i = path.lastIndexOf(File.separator);
        File f;
        if (i > 0) {
            String p = path.substring(0, i);
            f = new File(p);
            f.mkdirs();
        }
        f = new File(path);
        if (!f.exists() || !(f.length() == length)) {
            Client.getInstance().getObserver().fireFileCreationEvent(path, length);
            f.createNewFile();
            FileChannel fc = new FileOutputStream(f).getChannel();
            fc.position(length - 1);
            fc.write(ByteBuffer.allocate(1));
            fc.close();
            return true;
        }
        return false;
    }

    /**
     * @param off
     */
    public void setOffset(int off) {
        offset = off;
    }
    
    /**
     * @return
     */
    public int getOffset() {
        return offset;
    }
    
    /**
     * @param p
     */
    public void setPath(String p) {
        path = p;
    }
    
    /**
     * @param l
     */
    public void setLength(int l) {
        length = l;
    }
    
    /**
     * @param i
     */
    public void setIndex(int i) {
        index = i;
    }
    
    /**
     * @param i
     */
    public void setPieceIndex(int i) {
        pieceIndex = i;
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
    public int getIndex() {
        return index;
    }
    
    /**
     * @return
     */
    public int getLength() {
        return length;
    }
    
    /**
     * @return
     */
    public int getPieceIndex() {
        return pieceIndex;
    }

    /**
     * @param s
     * @return
     */
    public int getLastPieceIndex(int s) {
        int d = s - getOffset();
        int j = getPieceIndex() + 1;
        while (d < getLength()) {
            d += s;
            j++;
        }
        return j - 1;
    }

    /**
     * @param i
     * @param s
     * @return
     */
    public int getFileOffset(int i) {
        if (getPieceIndex() >= i) {
            throw new IllegalArgumentException();
        }
        int s = Client.getInstance().getDownloadItem().getPieceLength();
        int d = s - getOffset();
        int j = getPieceIndex() + 1;
        while (i > j) {
            d += s;
            j++;
        }
        return d;
    }
    
    public boolean equals(Object o) {
        if (o == null || !(o instanceof BTFile)) {
            return false;
        }
        BTFile f = (BTFile) o;
        return index == f.getIndex();
    }
    
    public int hashCode() {
        return new Integer(index).hashCode();
    }
}
