package hpbtc.processor;

import hpbtc.protocol.torrent.Peer;
import java.net.InetSocketAddress;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu
 */
public class PeerRepository {

    private Map<InetSocketAddress, Peer> peerInfo;

    public PeerRepository() {
        peerInfo = new HashMap<InetSocketAddress, Peer>();
    }

    public boolean isHandshakeReceived(InetSocketAddress address) {
        return peerInfo.get(address) != null;
    }

    public void removePeer(InetSocketAddress address) {
        peerInfo.remove(address);
    }

    public void addPeer(InetSocketAddress address, byte[] peerId, byte[] infoHash) {
        Peer p = new Peer(address, peerId);
        p.setInfoHash(infoHash);
        peerInfo.put(address, p);
    }

    public boolean isMessagesReceived(InetSocketAddress address) {
        Peer peer = peerInfo.get(address);
        return peer == null ? false : peer.isMessagesReceived();
    }

    public void setMessagesReceived(InetSocketAddress address) {
        peerInfo.get(address).setMessagesReceived();
    }

    public byte[] getInfoHash(InetSocketAddress address) {
        return peerInfo.get(address).getInfoHash();
    }
    
    public void setPieces(InetSocketAddress address, BitSet pieces) {
        peerInfo.get(address).setPieces(pieces);
    }
    
    public void setPiece(InetSocketAddress address, int index) {
        peerInfo.get(address).setPiece(index);
    }
    
    public void setPeerChoking(InetSocketAddress address, boolean choking) {
        peerInfo.get(address).setPeerChoking(choking);
    }
    
    public void setPeerInterested(InetSocketAddress address, boolean interested) {
        peerInfo.get(address).setPeerInterested(interested);
    }
}
