package hpbtc.processor;

import java.net.InetSocketAddress;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Cristian Mocanu
 */
public class PeerRepository {

    private Map<byte[], Set<PeerInfo>> peerInfo;

    public PeerRepository() {
        peerInfo = new HashMap<byte[], Set<PeerInfo>>();
    }

    private PeerInfo getPeer(InetSocketAddress address) {
        for (Set<PeerInfo> s : peerInfo.values()) {
            for (PeerInfo p : s) {
                if (p.getAddress().equals(address)) {
                    return p;
                }
            }
        }
        return null;
    }

    public boolean isHandshakeReceived(InetSocketAddress address) {
        return getPeer(address) != null;
    }

    public void removePeer(InetSocketAddress address) {
        for (Set<PeerInfo> s : peerInfo.values()) {
            Iterator<PeerInfo> pIt = s.iterator();
            while (pIt.hasNext()) {
                PeerInfo p = pIt.next();
                if (p.getAddress().equals(address)) {
                    pIt.remove();
                }
            }
        }
    }

    public void addPeer(InetSocketAddress address, byte[] peerId, byte[] infoHash) {
        Set<PeerInfo> p = peerInfo.get(infoHash);
        if (p == null) {
            p = new HashSet<PeerInfo>();
            peerInfo.put(infoHash, p);
        }
        p.add(new PeerInfo(address, peerId));
    }

    public boolean isMessagesReceived(InetSocketAddress address) {
        PeerInfo peer = getPeer(address);
        return peer == null ? false : peer.isMessagesReceived();
    }

    public void setMessagesReceived(InetSocketAddress address) {
        getPeer(address).setMessagesReceived();
    }

    public byte[] getInfoHash(InetSocketAddress address) {
        for (byte[] b : peerInfo.keySet()) {
            for (PeerInfo p: peerInfo.get(b)) {
                if (p.getAddress().equals(address)) {
                    return b;
                }
            }
        }
        return null;
    }
    
    public void setPieces(InetSocketAddress address, BitSet pieces) {
        getPeer(address).setPieces(pieces);
    }
}
