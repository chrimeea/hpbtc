/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol.torrent;

/**
 * @author chris
 *
 */
public class Peer {

    private String peerId;
    private String ip;
    private int port;
    
    /**
     * @param i
     * @param p
     * @param id
     */
    public Peer(String i, int p, String id) {
        peerId = id;
        ip = i;
        port = p;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        Peer p;
        if (o instanceof Peer) {
            p = (Peer) o;
        } else {
            return false;
        }
        if (ip.equals(p.ip) && port == p.port) {
            return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (ip + "#" + port).hashCode();
    }
    
    /**
     * @return Returns the ip.
     */
    public String getIp() {
        return ip;
    }
    
    public void setId(String id) {
        peerId = id;
    }
    
    /**
     * @return Returns the peerId.
     */
    public String getId() {
        return peerId;
    }
    
    /**
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return ip;
    }
}
