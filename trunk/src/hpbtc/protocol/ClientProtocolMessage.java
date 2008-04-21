package hpbtc.protocol;

import hpbtc.protocol.message.ProtocolMessage;

/**
 *
 * @author Cristian Mocanu
 */
public class ClientProtocolMessage {

    private ProtocolMessage message;
    private Peer source;
    
    public ClientProtocolMessage(ProtocolMessage message, Peer source) {
        this.message = message;
        this.source = source;
    }

    public ProtocolMessage getMessage() {
        return message;
    }

    public Peer getSource() {
        return source;
    }
}
