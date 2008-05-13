package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.CancelMessage;
import hpbtc.protocol.message.ChokeMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.InterestedMessage;
import hpbtc.protocol.message.NotInterestedMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.RequestMessage;
import hpbtc.protocol.message.UnchokeMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.network.RawMessage;
import hpbtc.protocol.torrent.TorrentInfo;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Protocol {

    private static Logger logger = Logger.getLogger(Protocol.class.getName());
    
    private List<TorrentInfo> torrents;
    private Network network;
    private List<InetSocketAddress> connected;
    
    public Protocol() {
        torrents = new LinkedList<TorrentInfo>();
        network = new Network();
        connected = new LinkedList<InetSocketAddress>();
    }
    
    public void download(String fileName) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        torrents.add(new TorrentInfo(fis));
        fis.close();
    }

    public void startProtocol() throws IOException {
        network.connect();
        new Thread(new Runnable() {

            public void run() {
                synchronized (network) {
                    do {
                        do {
                            try {
                                network.wait();
                            } catch (InterruptedException e) {
                            }
                        } while (!network.hasUnreadMessages());
                        RawMessage message = null;
                        try {
                            message = network.takeMessage();
                            process(message);
                        } catch (IOException ioe) {
                            logger.warning(ioe.getLocalizedMessage());
                            try {
                                network.closeConnection(message.getPeer());
                            } catch (IOException e) {
                                logger.warning(e.getLocalizedMessage());
                            }
                        }
                    } while (network.hasUnreadMessages());
                }
            }
        }).start();
    }
    
    private void process(RawMessage data) throws IOException {
        if (data.isDisconnect()) {
            disconnectedByPeer(data.getPeer());
            return;
        }
        ByteBuffer current = ByteBuffer.wrap(data.getMessage());
        do {
            if (connected.contains(data.getPeer())) {
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
            } else {
                process(new HandshakeMessage(current));
            }
        } while (current.remaining() > 0);
    }
    
    private void disconnectedByPeer(InetSocketAddress address) {
    }

    private void process(HandshakeMessage message) {
    }
    
    private void process(BitfieldMessage message) {
    }
    
    private void process(CancelMessage message) {
    }
    
    private void process(ChokeMessage message) {
    }
    
    private void process(HaveMessage message) {
    }

    private void process(InterestedMessage message) {
    }
    
    private void process(NotInterestedMessage message) {
    }
    
    private void process(PieceMessage message) {
    }
    
    private void process(RequestMessage message) {
    }
    
    private void process(UnchokeMessage message) {
    }
}
