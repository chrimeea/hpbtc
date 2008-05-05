package hpbtc.protocol.network;

import java.net.InetSocketAddress;

/**
 *
 * @author Cristian Mocanu
 */
public class ClientProtocolMessage {
    
    private byte[] message;
    private InetSocketAddress peer;
    
    public ClientProtocolMessage(InetSocketAddress peer, byte[] message) {
        this.message = message;
        this.peer = peer;
    }
    
    public byte[] getMessage() {
        return message;
    }
    
    public InetSocketAddress getPeer() {
        return peer;
    }
}
