package hpbtc.processor;

import hpbtc.protocol.torrent.Peer;
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
import hpbtc.protocol.message.ProtocolMessage;
import hpbtc.protocol.message.RequestMessage;
import hpbtc.protocol.message.UnchokeMessage;
import hpbtc.protocol.network.Network;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 *
 * @author Cristian Mocanu
 */
public class RawMessageProcessor {

    private Network client;
    private Peer peer;
    private boolean handshakeReceived;
    private ByteBuffer current;

    public RawMessageProcessor(Network client, Peer peer) {
        this.client = client;
        this.peer = peer;
    }

    public RawMessageProcessor(Network client, InetSocketAddress address) {
        this.client = client;
        peer = new Peer(address);
    }

    public void process(byte[] data) throws IOException {
        current = ByteBuffer.wrap(data);
        do {
            if (!handshakeReceived) {
                process(new HandshakeMessage(current));
            } else {
                byte disc = current.get();
                switch (disc) {
                    case BitfieldMessage.TYPE_DISCRIMINATOR:
                        process(new BitfieldMessage(current));
                        break;
                    case CancelMessage.TYPE_DISCRIMINATOR:
                        process(new CancelMessage(current));
                        break;
                    case ChokeMessage.TYPE_DISCRIMINATOR:
                        process(new ChokeMessage());
                        break;
                    case HaveMessage.TYPE_DISCRIMINATOR:
                        process(new HaveMessage(current));
                        break;
                    case InterestedMessage.TYPE_DISCRIMINATOR:
                        process(new InterestedMessage());
                        break;
                    case NotInterestedMessage.TYPE_DISCRIMINATOR:
                        process(new NotInterestedMessage());
                        break;
                    case PieceMessage.TYPE_DISCRIMINATOR:
                        process(new PieceMessage(current));
                        break;
                    case RequestMessage.TYPE_DISCRIMINATOR:
                        process(new RequestMessage(current));
                        break;
                    case UnchokeMessage.TYPE_DISCRIMINATOR:
                        process(new UnchokeMessage());
                }
            }
        } while (current.remaining() > 0);
    }

    private void process(BitfieldMessage message) {
    }

    private void process(CancelMessage message) {
    }

    private void process(ChokeMessage message) {
    }

    private void process(HandshakeMessage message) {
        handshakeReceived = true;
    }

    private void process(HaveMessage message) {
    }

    private void process(IdleMessage message) {
    }

    private void process(InterestedMessage message) {
    }

    private void process(NotInterestedMessage message) {
    }

    private void process(PIDMessage message) {
    }

    private void process(final PieceMessage message) {
    }

    private void process(RequestMessage message) {
    }

    private void process(UnchokeMessage message) {
    }
}
