/*
 * Created on Mar 7, 2006
 *
 */
package hpbtc.message;

/**
 * @author chris
 *
 */
public class MessageFactory {
    
    public static ProtocolMessage createMessage(byte type) {
        ProtocolMessage pm = null;
        switch (type) {
        case ProtocolMessage.TYPE_BITFIELD: pm = new BitfieldMessage();break;
        case ProtocolMessage.TYPE_CANCEL: pm =  new CancelMessage();break;
        case ProtocolMessage.TYPE_CHOKE: pm = new ChokeMessage();break;
        case ProtocolMessage.TYPE_HAVE: pm = new HaveMessage();break;
        case ProtocolMessage.TYPE_INTERESTED: pm = new InterestedMessage();break;
        case ProtocolMessage.TYPE_NOT_INTERESTED: pm = new NotInterestedMessage();break;
        case ProtocolMessage.TYPE_PIECE: pm = new PieceMessage();break;
        case ProtocolMessage.TYPE_REQUEST: pm = new RequestMessage();break;
        case ProtocolMessage.TYPE_UNCHOKE: pm = new UnchokeMessage();
        }
        return pm;
    }
}
