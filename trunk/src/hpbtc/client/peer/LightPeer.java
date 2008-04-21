/*
 * Created on Jan 27, 2006
 *
 */
package hpbtc.client.peer;

import hpbtc.protocol.torrent.Peer;

/**
 * @author chris
 *
 */
public class LightPeer {

    private Peer peer;
    private int totalPieces;
    private int uploadRate;
    private int downloadRate;
    private boolean choked;
    private boolean free;
    private boolean snubbed;
    private boolean chokedHere;
    
    /**
     * @param p
     */
    public LightPeer(Peer p) {
        free = p.isFree();
        peer = p;
        choked = true;
        snubbed = p.isSnubbed();
        chokedHere = p.isChokedHere();
    }
    
    public boolean isChokedHere() {
        return chokedHere;
    }
    
    public boolean isSnubbed() {
        return snubbed;
    }
    
    /**
     * @return
     */
    public boolean isFree() {
        return free;
    }
    
    /**
     * @param c
     */
    public void setChoked(boolean c) {
        choked = c;
    }
    
    /**
     * @return
     */
    public boolean isChoked() {
        return choked;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return peer.hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (peer == null && o != null) {
            return false;
        }
        return peer.equals(o);
    }
    
    /**
     * @param t
     */
    public void setTotalPieces(int t) {
        totalPieces = t;
    }
    
    /**
     * @return
     */
    public int getTotalPieces() {
        return totalPieces;
    }
    
    /**
     * @return
     */
    public Peer getPeer() {
        return peer;
    }
    
    /**
     * @param u
     */
    public void setUploadRate(int u) {
        uploadRate = u;
    }
    
    /**
     * @param d
     */
    public void setDownloadRate(int d) {
        downloadRate = d;
    }
    
    /**
     * @return
     */
    public int getUploadRate() {
        return uploadRate;
    }
    
    /**
     * @return
     */
    public int getDownloadRate() {
        return downloadRate;
    }
}
