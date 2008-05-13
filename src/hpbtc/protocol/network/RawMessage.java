package hpbtc.protocol.network;

import java.net.InetSocketAddress;

/**
 *
 * @author Cristian Mocanu
 */
public class RawMessage {
    
    private byte[] message;
    private InetSocketAddress peer;
    private boolean disconnect;
    
    public RawMessage(InetSocketAddress peer, byte[] message) {
        this.message = message;
        this.peer = peer;
    }
    
    public RawMessage(InetSocketAddress peer) {
        this.peer = peer;
        disconnect = true;
    }
    
    public boolean isDisconnect() {
        return disconnect;
    }
    
    public byte[] getMessage() {
        return message;
    }
    
    public InetSocketAddress getPeer() {
        return peer;
    }
}
