/*
 * Created on 17.10.2008
 */
package hpbtc.protocol.dht;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
class Bucket {

    private long lastChanged = System.currentTimeMillis();
    private List<DHTNode> nodes = new LinkedList<DHTNode>();
    private byte[] min;
    private byte[] max;

    Bucket() {
        min = new byte[20];
        max = new byte[20];
        Arrays.fill(max, (byte) 255);
    }

    Bucket(byte[] min, byte[] max) {
        this.min = min;
        this.max = max;
    }
    
    byte[] getMin() {
        return min;
    }
    
    byte[] getMax() {
        return max;
    }
    
    boolean insertNode(DHTNode node) {
        if (nodes.size() == 8) {
            Iterator<DHTNode> it = nodes.iterator();
            boolean hasBad = false;
            while (it.hasNext()) {
                DHTNode n = it.next();
                if (n.getStatus() == DHTNode.Status.BAD) {
                    it.remove();
                    hasBad = true;
                    break;
                }
            }
            if (!hasBad) {
                return false;
            }
        }
        nodes.add(node);
        lastChanged = System.currentTimeMillis();
        return true;
    }

    List<DHTNode> getNodes() {
        return nodes;
    }
}
