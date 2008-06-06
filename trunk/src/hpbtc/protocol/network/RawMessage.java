package hpbtc.protocol.network;

import java.net.InetSocketAddress;

/**
 *
 * @author Cristian Mocanu
 */
public class RawMessage {
    
    private byte[] message;
    private InetSocketAddress peerAddress;
    private boolean disconnect;
    
    public RawMessage(InetSocketAddress peer, byte[] message) {
        this.message = message;
        this.peerAddress = peer;
    }
    
    public RawMessage(InetSocketAddress peer) {
        this.peerAddress = peer;
        disconnect = true;
    }
    
    public boolean isDisconnect() {
        return disconnect;
    }
    
    public byte[] getMessage() {
        return message;
    }
    
    public InetSocketAddress getPeerAddress() {
        return peerAddress;
    }
}
