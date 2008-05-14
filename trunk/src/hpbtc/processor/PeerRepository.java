package hpbtc.processor;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Cristian Mocanu
 */
public class PeerRepository {

    private Set<PeerInfo> peerInfo;
    
    public PeerRepository() {
        peerInfo = new HashSet<PeerInfo>();
    }
    
    private PeerInfo getPeer(InetSocketAddress address) {
        for (PeerInfo p: peerInfo) {
            if (p.getAddress().equals(address)) {
                return p;
            }
        }
        return null;
    }
    
    public boolean isHandshakeReceived(InetSocketAddress address) {
        return getPeer(address) != null;
    }
    
    public void removePeer(InetSocketAddress address) {
        Iterator<PeerInfo> pIt = peerInfo.iterator();
        while (pIt.hasNext()) {
            PeerInfo p = pIt.next();
            if (p.getAddress().equals(address)) {
                pIt.remove();
            }
        }
    }
    
    public void addPeer(InetSocketAddress address, byte[] peerId, byte[] infoHash) {
        peerInfo.add(new PeerInfo(address, peerId, infoHash));
    }
    
    public boolean isMessagesReceived(InetSocketAddress address) {
        PeerInfo peer = getPeer(address);
        return peer == null ? false : peer.isMessagesReceived();
    }
    
    public void setMessagesReceived(InetSocketAddress address) {
        getPeer(address).setMessagesReceived();
    }
    
    public byte[] getInfoHash(InetSocketAddress address) {
        return getPeer(address).getInfoHash();
    }
}