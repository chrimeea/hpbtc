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
        case BitfieldMessage.TYPE_DISCRIMINATOR: pm = new BitfieldMessage(message);break;
        case CancelMessage.TYPE_DISCRIMINATOR: pm =  new CancelMessage(message);break;
        case ChokeMessage.TYPE_DISCRIMINATOR: pm = new ChokeMessage();break;
        case HaveMessage.TYPE_DISCRIMINATOR: pm = new HaveMessage(message);break;
        case InterestedMessage.TYPE_DISCRIMINATOR: pm = new InterestedMessage();break;
        case NotInterestedMessage.TYPE_DISCRIMINATOR: pm = new NotInterestedMessage();break;
        case PieceMessage.TYPE_DISCRIMINATOR: pm = new PieceMessage(message);break;
        case RequestMessage.TYPE_DISCRIMINATOR: pm = new RequestMessage(message);break;
        case UnchokeMessage.TYPE_DISCRIMINATOR: pm = new UnchokeMessage();
        }
        return pm;
    }
}
