package hpbtc.processor;

import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageWriterImpl implements MessageWriter {

    private static Logger logger = Logger.getLogger(MessageWriterImpl.class.
            getName());
    private Map<Peer, Queue<SimpleMessage>> messagesToSend;
    private ByteBuffer currentWrite;
    private Register register;
    private Map<byte[], Torrent> torrents;

    public MessageWriterImpl(final Map<byte[], Torrent> torrents,
            final Register register) {
        this.torrents = torrents;
        messagesToSend = new Hashtable<Peer, Queue<SimpleMessage>>();
        this.register = register;
    }

    public void cancelPieceMessage(final Peer peer) {
        Queue<SimpleMessage> q = messagesToSend.get(peer);
        if (q != null) {
            Iterator<SimpleMessage> i = q.iterator();
            while (i.hasNext()) {
                SimpleMessage m = i.next();
                if (m.getMessageType() == SimpleMessage.TYPE_PIECE) {
                    i.remove();
                    PieceMessage pm = (PieceMessage) m;
                    Peer p = pm.getDestination();
                    Torrent t = torrents.get(p.getInfoHash());
                    peer.removeRequest(pm.getIndex(), pm.getBegin(),
                            t.getChunkSize());
                }
            }
        }
    }

    public void cancelPieceMessage(final int begin, final int index,
            final int length, final Peer peer) {
        Queue<SimpleMessage> q = messagesToSend.get(peer);
        if (q != null) {
            Iterator<SimpleMessage> i = q.iterator();
            while (i.hasNext()) {
                SimpleMessage m = i.next();
                if (m.getMessageType() == SimpleMessage.TYPE_PIECE) {
                    PieceMessage pm = (PieceMessage) m;
                    if (pm.getIndex() == index && pm.getBegin() == begin &&
                            pm.getLength() == length) {
                        i.remove();
                        Peer p = pm.getDestination();
                        Torrent t = torrents.get(p.getInfoHash());
                        p.removeRequest(pm.getIndex(), pm.getBegin(),
                                t.getChunkSize());
                    }
                }
            }
        }
    }

    public void disconnect(final Peer peer) throws IOException {
        messagesToSend.remove(peer);
        torrents.get(peer.getInfoHash()).removePeer(peer);
        peer.disconnect();
    }

    public void writeNext(final Peer peer) throws IOException {
        if (currentWrite == null || currentWrite.remaining() == 0) {
            SimpleMessage sm = null;
            sm = messagesToSend.get(peer).poll();
            currentWrite = sm.send();
            currentWrite.rewind();
            logger.fine("Sending: " + sm);
        }
        peer.upload(currentWrite);
    }

    public void postMessage(final SimpleMessage message) throws
            IOException {
        Peer peer = message.getDestination();
        Queue<SimpleMessage> q = messagesToSend.get(peer);
        if (q == null) {
            q = new ConcurrentLinkedQueue<SimpleMessage>();
            messagesToSend.put(peer, q);
        }
        q.add(message);
        register.registerWrite(peer);
    }

    public boolean isEmpty(final Peer peer) {
        return messagesToSend.get(peer).isEmpty();
    }
}