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
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Protocol {

    private static Logger logger = Logger.getLogger(Protocol.class.getName());
    
    private Set<TorrentInfo> torrents;
    private Network network;
    private PeerRepository peerRep;
    private byte[] peerId;
    
    public Protocol() {
        torrents = new HashSet<TorrentInfo>();
        network = new Network();
        peerRep = new PeerRepository();
        generateId();
    }
    
    private void generateId() {
        peerId = new byte[20];
        peerId[0] = 'C';
        peerId[1] = 'M';
        peerId[2] = '-';
        peerId[3] = '2';
        peerId[4] = '.';
        peerId[5] = '0';
        Random r = new Random();
        r.nextBytes(peerId);
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
            peerRep.removePeer(data.getPeer());
            return;
        }
        ByteBuffer current = ByteBuffer.wrap(data.getMessage());
        InetSocketAddress address = data.getPeer();
        do {
            if (peerRep.isHandshakeReceived(address)) {
                byte disc = current.get();
                switch (disc) {
                    case BitfieldMessage.TYPE_DISCRIMINATOR:
                        process(new BitfieldMessage(current), address);
                        break;
                    case CancelMessage.TYPE_DISCRIMINATOR:
                        process(new CancelMessage(current), address);
                        break;
                    case ChokeMessage.TYPE_DISCRIMINATOR:
                        process(new ChokeMessage(), address);
                        break;
                    case HaveMessage.TYPE_DISCRIMINATOR:
                        process(new HaveMessage(current), address);
                        break;
                    case InterestedMessage.TYPE_DISCRIMINATOR:
                        process(new InterestedMessage(), address);
                        break;
                    case NotInterestedMessage.TYPE_DISCRIMINATOR:
                        process(new NotInterestedMessage(), address);
                        break;
                    case PieceMessage.TYPE_DISCRIMINATOR:
                        process(new PieceMessage(current), address);
                        break;
                    case RequestMessage.TYPE_DISCRIMINATOR:
                        process(new RequestMessage(current), address);
                        break;
                    case UnchokeMessage.TYPE_DISCRIMINATOR:
                        process(new UnchokeMessage(), address);
                }
            } else {
                process(new HandshakeMessage(current), data.getPeer());
            }
        } while (current.remaining() > 0);
    }

    private void process(HandshakeMessage message, InetSocketAddress address) throws IOException {
        if (torrents.contains(message.getInfoHash())) {
            peerRep.addPeer(address, message.getPeerId());
            HandshakeMessage reply = new HandshakeMessage(message.getInfoHash(), peerId);
            network.postMessage(address, reply.send());
        } else {
            network.closeConnection(address);
        }
    }

    private void process(BitfieldMessage message, InetSocketAddress address) {
    }
    
    private void process(CancelMessage message, InetSocketAddress address) {
    }
    
    private void process(ChokeMessage message, InetSocketAddress address) {
    }
    
    private void process(HaveMessage message, InetSocketAddress address) {
    }

    private void process(InterestedMessage message, InetSocketAddress address) {
    }
    
    private void process(NotInterestedMessage message, InetSocketAddress address) {
    }
    
    private void process(PieceMessage message, InetSocketAddress address) {
    }
    
    private void process(RequestMessage message, InetSocketAddress address) {
    }
    
    private void process(UnchokeMessage message, InetSocketAddress address) {
    }
}
