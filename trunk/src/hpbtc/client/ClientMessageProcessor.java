package hpbtc.client;

import hpbtc.protocol.message.MessageProcessor;
import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.CancelMessage;
import hpbtc.protocol.message.ChokeMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.IdleMessage;
import hpbtc.protocol.message.InterestedMessage;
import hpbtc.protocol.message.NotInterestedMessage;
import hpbtc.protocol.message.PIDMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.RequestMessage;
import hpbtc.protocol.message.UnchokeMessage;

/**
 *
 * @author Cristian Mocanu
 */
public class ClientMessageProcessor implements MessageProcessor {

    public ClientMessageProcessor(Peer messagePeer) {
    }

    public void process(BitfieldMessage message) {
    }

    public void process(CancelMessage message) {
    }

    public void process(ChokeMessage message) {
    }

    public void process(HandshakeMessage message) {
    }

    public void process(HaveMessage message) {
    }

    public void process(IdleMessage message) {
    }

    public void process(InterestedMessage message) {
    }

    public void process(NotInterestedMessage message) {
    }

    public void process(PIDMessage message) {
    }

    public void process(final PieceMessage message) {
    }

    public void process(RequestMessage message) {
    }

    public void process(UnchokeMessage message) {
    }
}
