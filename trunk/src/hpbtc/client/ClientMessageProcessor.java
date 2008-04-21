package hpbtc.client;

import hpbtc.protocol.Client;
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
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.PeerConnection;
import hpbtc.client.piece.Piece;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class ClientMessageProcessor implements MessageProcessor {

    private static Logger logger = Logger.getLogger(ClientMessageProcessor.class.getName());
    
    private DownloadItem item;
    private Peer peer;

    public ClientMessageProcessor(Peer messagePeer) {
        Client client = Client.getInstance();
        item = client.getDownloadItem();
        this.peer = messagePeer;
    }

    public void process(BitfieldMessage message) {
        BitSet bs = message.getBitfield();
        int total = item.getTotalPieces();
        for (int i = bs.nextSetBit(0); i >= 0 && i < total; i = bs.nextSetBit(i + 1)) {
            item.getPiece(i).addPeer(peer);
            peer.addPiece(i);
        }
    }

    public void process(CancelMessage message) {
        peer.getConnection().cancelRequestReceived(message);
    }

    public void process(ChokeMessage message) {
        peer.setChokedThere(true);
    }

    public void process(HandshakeMessage message) {
        if (message.getInfoHash().equals(ByteBuffer.wrap(item.getInfoHash()))) {
            logger.info("handshake ok " + peer);
            PeerConnection pc = peer.getConnection();
            if (!pc.isHandshakeSent()) {
                PIDMessage pm = new PIDMessage(Client.getInstance().getPIDBytes());
                pc.addUploadMessage(message);
                pc.addUploadMessage(pm);
                pc.setHandshakeSent(true);
            }
            pc.close();
            return;
        }
        logger.info("handshake error " + peer);
    }

    public void process(HaveMessage message) {
        int index = message.getIndex();
        if (!peer.hasPiece(index)) {
            item.getPiece(index).addPeer(peer);
            peer.addPiece(index);
        }
        item.findAllPieces();
    }

    public void process(IdleMessage message) {
    }

    public void process(InterestedMessage message) {
        peer.setInterestedThere(true);
    }

    public void process(NotInterestedMessage message) {
        peer.setInterestedThere(false);
    }

    public void process(PIDMessage message) {
        PeerConnection pc = peer.getConnection();
        if (pc != null && pc.getPeer().getId() == null) {
            String pid;
            try {
                ByteBuffer p = message.getPid();
                pid = new String(p.array(), p.arrayOffset(), 20, "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                return;
            }
            peer.setId(pid);
            checked();
        } else {
            ByteBuffer pid;
            try {
                pid = ByteBuffer.wrap(peer.getId().getBytes("ISO-8859-1"));
            } catch (UnsupportedEncodingException e) {
                return;
            }
            if (message.getPid().equals(pid)) {
                checked();
                return;
            }
            logger.info("pid error " + peer);
            pc.close();
        }
    }

    private void checked() {
        logger.info("pid ok " + peer);
        PeerConnection pc = peer.getConnection();
        pc.addUploadMessage(new BitfieldMessage());
        pc.startAllTimers();
    }

    public void process(final PieceMessage message) {
        final int index = message.getIndex();
        final int begin = message.getBegin();
        int length = message.getLength();
        final Piece pe = item.getPiece(index);
        if (pe.requestDone(message)) {
            peer.requestDone();
        }
        if (!pe.haveAll(begin, length)) {
            item.getRateTimer().schedule(new TimerTask() {

                public void run() {
                    if (pe.savePiece(begin, message.getPiece())) {
                        item.broadcastHave(index);
                    }
                    item.findNextPiece();
                }
            }, 0);
        }
    }

    public void process(RequestMessage message) {
        int index = message.getIndex();
        int begin = message.getBegin();
        int length = message.getLength();
        PeerConnection c = peer.getConnection();
        if (c != null && !peer.isChokedHere() && item.getPiece(index).isComplete()) {
            try {
            ByteBuffer p = item.getPiece(index).getPiece(begin, length);
            p.rewind();
            PieceMessage pm = new PieceMessage(begin, index, length, p);
            c.addUploadMessage(pm);
            } catch (IOException e) {
                //TODO: report exception
            }
        }
    }

    public void process(UnchokeMessage message) {
        peer.setChokedThere(false);
        item.findAllPieces();
    }
}
