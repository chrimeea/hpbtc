package hpbtc.client.piece;

import hpbtc.client.peer.LightPeer;

import java.util.List;

/**
 * @author chris
 *
 */
public class LightPiece {

    private Piece piece;
    private List<LightPeer> peers;
    
    /**
     * @param p
     */
    public LightPiece(Piece p) {
        piece = p;
    }
    
    /**
     * @return
     */
    public Piece getPiece() {
        return piece;
    }
    
    /**
     * @param lp
     */
    public void setPeers(List<LightPeer> lp) {
        peers = lp;
    }
    
    /**
     * @return
     */
    public List<LightPeer> getPeers() {
        return peers;
    }
    
    /**
     * @return
     */
    public int getAvailability() {
        int i = 0;
        for (LightPeer lp : peers) {
            if (lp.isFree()) {
                i++;
            }
        }
        return i;
    }
}
