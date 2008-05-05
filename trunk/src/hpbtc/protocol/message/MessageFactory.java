/*
 * Created on Mar 7, 2006
 *
 */
package hpbtc.protocol.message;

import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class MessageFactory {
    
    public static ProtocolMessage createMessage(ByteBuffer message) {
        ProtocolMessage pm = null;
        byte current = message.get();
        switch (current) {
        case ProtocolMessage.TYPE_BITFIELD: pm = new BitfieldMessage(message);break;
        case ProtocolMessage.TYPE_CANCEL: pm =  new CancelMessage(message);break;
        case ProtocolMessage.TYPE_CHOKE: pm = new ChokeMessage();break;
        case ProtocolMessage.TYPE_HAVE: pm = new HaveMessage(message);break;
        case ProtocolMessage.TYPE_INTERESTED: pm = new InterestedMessage();break;
        case ProtocolMessage.TYPE_NOT_INTERESTED: pm = new NotInterestedMessage();break;
        case ProtocolMessage.TYPE_PIECE: pm = new PieceMessage(message);break;
        case ProtocolMessage.TYPE_REQUEST: pm = new RequestMessage(message);break;
        case ProtocolMessage.TYPE_UNCHOKE: pm = new UnchokeMessage();
        }
        return pm;
    }
}
