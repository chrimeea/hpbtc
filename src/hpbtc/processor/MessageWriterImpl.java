package hpbtc.processor;

import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.NetworkWriter;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class MessageWriterImpl implements MessageWriter {

    private static Logger logger = Logger.getLogger(MessageWriterImpl.class.getName());
    private Map<Peer, Queue<SimpleMessage>> messagesToSend;
    private ByteBuffer currentWrite;
    private NetworkWriter network;

    public MessageWriterImpl() {
        messagesToSend = new ConcurrentHashMap<Peer, Queue<SimpleMessage>>();
        network = new NetworkWriter(this);
    }

    public int connect() throws IOException {
        return network.connect();
    }
    
    public void cancelPieceMessage(int begin, int index, int length, Peer peer) {
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

    public void closeConnection(Peer peer) throws IOException {
        messagesToSend.remove(peer);
        ByteChannel ch = peer.getChannel();
        if (ch != null) {
            ch.close();
        }
    }

    public void writeNext(Peer peer) throws IOException {
        if (currentWrite == null || currentWrite.remaining() == 0) {
            Queue<SimpleMessage> q = messagesToSend.get(peer);
            SimpleMessage sm = q.poll();
            currentWrite = sm.send();
        }
        try {
            peer.upload(currentWrite);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getLocalizedMessage(), e);
            closeConnection(peer);
        }
    }

    public void postMessage(SimpleMessage message) throws IOException {
        Peer peer = message.getDestination();
        Queue<SimpleMessage> q = messagesToSend.get(peer);
        if (q == null) {
            q = new ConcurrentLinkedQueue<SimpleMessage>();
            messagesToSend.put(peer, q);
        }
        q.add(message);
        network.registerNow(peer, SelectionKey.OP_WRITE);
    }
    
    public boolean isEmpty(Peer peer) {
        return messagesToSend.get(peer).isEmpty();
    }
    
    public void disconnect() {
        network.disconnect();
    }
}
