package hpbtc.processor;

import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import hpbtc.protocol.torrent.Tracker;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    
    public State()
            throws UnsupportedEncodingException {
        messagesToSend = new Hashtable<Peer, Queue<SimpleMessage>>();
        this.peerId = TorrentUtil.generateId();
        protocol = TorrentUtil.getSupportedProtocol();
        trackers = new Hashtable<byte[], Tracker>();
        torrents = new Hashtable<byte[], Torrent>();
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
    }
    
    public void disconnect(final Peer peer)
            throws IOException {
        torrents.get(peer.getInfoHash()).removePeer(peer);
        messagesToSend.remove(peer);
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
