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
    
    public PeerInfo getPeer(InetSocketAddress address) {
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
    
    public void addPeer(PeerInfo peer) {
        peerInfo.add(peer);
    }
}
