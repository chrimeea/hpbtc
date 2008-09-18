package hpbtc.processor;

import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import hpbtc.protocol.torrent.Tracker;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class State {

    private byte[] protocol;
    private byte[] peerId;
    private Map<byte[], Torrent> torrents;
    private Map<byte[], Tracker> trackers;
    private Map<Peer, Queue<SimpleMessage>> messagesToSend;
    private Map<byte[], AtomicIntegerArray> availability;
    
    public State() throws UnsupportedEncodingException {
        messagesToSend = new Hashtable<Peer, Queue<SimpleMessage>>();
        this.peerId = TorrentUtil.generateId();
        protocol = TorrentUtil.getSupportedProtocol();
        trackers = new Hashtable<byte[], Tracker>();
        torrents = new Hashtable<byte[], Torrent>();
        availability = new Hashtable<byte[], AtomicIntegerArray>();
    }

    public byte[] getProtocol() {
        return protocol;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public byte[] findInfoHash(final byte[] infoHash) {
        for (byte[] b: torrents.keySet()) {
            if (Arrays.equals(infoHash, b)) {
                return b;
            }
        }
        return null;
    }
    
    public Torrent getTorrent(final Peer peer) {
        return torrents.get(peer.getInfoHash());
    }
    
    public Tracker getTracker(final Torrent torrent) {
        return trackers.get(torrent.getInfoHash());
    }
    
    public void addTorrent(final Torrent torrent, final Tracker tracker) {
        byte[] infoHash = torrent.getInfoHash();
        torrents.put(infoHash, torrent);
        trackers.put(infoHash, tracker);
        availability.put(infoHash, new AtomicIntegerArray(torrent.getNrPieces()));
    }
    
    public void updateAvailability(final Peer peer) {
        AtomicIntegerArray a = availability.get(peer.getInfoHash());
        BitSet bs = peer.getPieces();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            a.getAndIncrement(i);
        }
    }
    
    public void updateAvailability(final Peer peer, int index) {
        AtomicIntegerArray a = availability.get(peer.getInfoHash());
        a.getAndIncrement(index);
    }
    
    public int getAvailability(final Peer peer, int index) {
        AtomicIntegerArray a = availability.get(peer.getInfoHash());
        return a.get(index);
    }
    
    public void disconnect(final Peer peer)
            throws IOException {
        byte[] infoHash = peer.getInfoHash();
        torrents.get(infoHash).removePeer(peer);
        messagesToSend.remove(peer);
        AtomicIntegerArray a = availability.get(infoHash);
        BitSet bs = peer.getPieces();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            a.getAndDecrement(i);
        }
        peer.disconnect();
    }
    
    public Iterable<SimpleMessage> listMessagesToSend(final Peer peer) {
        return messagesToSend.get(peer);
    }
    
    public void addMessageToSend(final SimpleMessage message) {
        Peer peer = message.getDestination();
        Queue<SimpleMessage> q = messagesToSend.get(peer);
        if (q == null) {
            q = new ConcurrentLinkedQueue<SimpleMessage>();
            messagesToSend.put(peer, q);
        }
        q.add(message);
    }
    
    public boolean isMessagesToSendEmpty(final Peer peer) {
        return messagesToSend.get(peer).isEmpty();
    }
    
    public SimpleMessage getMessageToSend(final Peer peer) {
        return messagesToSend.get(peer).poll();
    }
}
