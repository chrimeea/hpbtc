package hpbtc.processor;

import hpbtc.protocol.torrent.Peer;
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
import hpbtc.protocol.message.ProtocolMessage;
import hpbtc.protocol.message.RequestMessage;
import hpbtc.protocol.message.UnchokeMessage;
import hpbtc.protocol.network.Network;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class RawMessageProcessor implements MessageProcessor {

    private static Logger logger = Logger.getLogger(RawMessageProcessor.class.getName());
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
        try {
            do {
                if (!handshakeReceived) {
                    ProtocolMessage m = new HandshakeMessage(current);
                    handshakeReceived = true;
                } else {
                //receive message
                }
            } while (current.remaining() > 0);
        } catch (EOFException eofe) {
        } catch (IOException ioe) {
            client.closeConnection(peer.getAddress());
            logger.warning(ioe.getLocalizedMessage());
        }
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
