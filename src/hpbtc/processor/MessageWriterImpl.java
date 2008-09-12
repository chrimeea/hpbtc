package hpbtc.processor;

import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
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

    public MessageWriterImpl(final Register register) {
        messagesToSend = new ConcurrentHashMap<Peer, Queue<SimpleMessage>>();
        this.register = register;
    }
    
    public void cancelPieceMessage(final int begin, final int index,
            final int length, final Peer peer) {
        Queue<SimpleMessage> q = messagesToSend.get(peer);
        if (q != null) {
            Iterator<SimpleMessage> i = q.iterator();
            while (i.hasNext()) {
                SimpleMessage m = i.next();
                if (m instanceof PieceMessage) {
                    PieceMessage pm = (PieceMessage) m;
                    if (pm.getIndex() == index && pm.getBegin() == begin && pm.
                            getLength() == length) {
                        i.remove();
                    }
                }
            }
        }
    }

    public void closeConnection(final Peer peer) throws IOException {
        messagesToSend.remove(peer);
        ByteChannel ch = peer.getChannel();
        if (ch != null) {
            ch.close();
        }
    }

    public void writeNext(final Peer peer) throws IOException {
        if (currentWrite == null || currentWrite.remaining() == 0) {
            Queue<SimpleMessage> q = messagesToSend.get(peer);
            SimpleMessage sm = q.poll();
            currentWrite = sm.send();
            logger.info("Sending message type " + sm.getMessageType() + " to " +
                    peer);
        }
        try {
            peer.upload(currentWrite);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getLocalizedMessage(), e);
            closeConnection(peer);
        }
    }

    public void postMessage(final SimpleMessage message) throws IOException {
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
