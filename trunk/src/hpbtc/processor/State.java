package hpbtc.processor;

import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import hpbtc.protocol.torrent.Tracker;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerArray;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class State {

    private byte[] protocol;
    private byte[] peerId;
    private Map<byte[], StateData> data;
    
    public State() throws UnsupportedEncodingException {
        this.peerId = TorrentUtil.generateId();
        protocol = TorrentUtil.getSupportedProtocol();
        data = new Hashtable<byte[], StateData>();
    }

    public byte[] getProtocol() {
        return protocol;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public byte[] findInfoHash(final byte[] infoHash) {
        for (byte[] b: data.keySet()) {
            if (Arrays.equals(infoHash, b)) {
                return b;
            }
        }
        return null;
    }
    
    public Torrent getTorrent(final Peer peer) {
        return data.get(peer.getInfoHash()).getTorrent();
    }
    
    public Tracker getTracker(final Torrent torrent) {
        return data.get(torrent.getInfoHash()).getTracker();
    }
    
    public void addTorrent(final Torrent torrent, final Tracker tracker) {
        byte[] infoHash = torrent.getInfoHash();
        data.put(infoHash, new StateData(torrent, tracker,
                new AtomicIntegerArray(torrent.getNrPieces())));
    }
    
    public void updateAvailability(final Peer peer) {
        AtomicIntegerArray a = data.get(peer.getInfoHash()).getAvailability();
        BitSet bs = peer.getPieces();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            a.getAndIncrement(i);
        }
    }
    
    public void updateAvailability(final Peer peer, int index) {
        AtomicIntegerArray a = data.get(peer.getInfoHash()).getAvailability();
        a.getAndIncrement(index);
    }
    
    public int getAvailability(final Peer peer, int index) {
        AtomicIntegerArray a = data.get(peer.getInfoHash()).getAvailability();
        return a.get(index);
    }
    
    public void disconnect(final Peer peer)
            throws IOException {
        byte[] infoHash = peer.getInfoHash();
        data.get(infoHash).getTorrent().removePeer(peer);
        AtomicIntegerArray a = data.get(infoHash).getAvailability();
        BitSet bs = peer.getPieces();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            a.getAndDecrement(i);
        }
        peer.disconnect();
    }
                
    public int increaseOptimisticCounter(final Torrent torrent) {
        return data.get(torrent.getInfoHash()).increaseOptimisticCounter();
    }
    
    public void resetOptimisticCounter(final Torrent torrent) {
        data.get(torrent.getInfoHash()).setOptimisticCounter(0);
    }
    
    private class StateData {
        private Torrent torrent;
        private Tracker tracker;
        private AtomicIntegerArray availability;
        private int optimisticCounter;

        public StateData(final Torrent torrent, final Tracker tracker,
                final AtomicIntegerArray availability) {
            this.torrent = torrent;
            this.tracker = tracker;
            this.availability = availability;
        }
        
        public int increaseOptimisticCounter() {
            return ++optimisticCounter;
        }
        
        public void setOptimisticCounter(int optimisticCounter) {
            this.optimisticCounter = optimisticCounter;
        }
        
        public void setAvailability(final AtomicIntegerArray availability) {
            this.availability = availability;
        }

        public void setTorrent(final Torrent torrent) {
            this.torrent = torrent;
        }

        public void setTracker(final Tracker tracker) {
            this.tracker = tracker;
        }

        public int getOptimisticCounter() {
            return optimisticCounter;
        }
        
        public AtomicIntegerArray getAvailability() {
            return availability;
        }

        public Torrent getTorrent() {
            return torrent;
        }

        public Tracker getTracker() {
            return tracker;
        }
    }
}
