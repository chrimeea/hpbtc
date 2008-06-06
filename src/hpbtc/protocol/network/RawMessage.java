package hpbtc.protocol.network;

import hpbtc.protocol.torrent.Peer;

/**
 *
 * @author Cristian Mocanu
 */
public class RawMessage {
    
    private byte[] message;
    private Peer peer;
    private boolean disconnect;
    
    public RawMessage(Peer peer, byte[] message) {
        this.message = message;
        this.peer = peer;
    }
    
    public RawMessage(Peer peer) {
        this.peer = peer;
        disconnect = true;
    }
    
    public boolean isDisconnect() {
        return disconnect;
    }
    
    public byte[] getMessage() {
        return message;
    }
    
    public Peer getPeer() {
        return peer;
    }
}
