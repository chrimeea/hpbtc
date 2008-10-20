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
    private List<Node> nodes = new LinkedList<Node>();
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
    
    boolean insertNode(Node node) {
        if (nodes.size() == 8) {
            Iterator<Node> it = nodes.iterator();
            boolean hasBad = false;
            while (it.hasNext()) {
                Node n = it.next();
                if (n.getStatus() == Node.Status.BAD) {
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

    List<Node> getNodes() {
        return nodes;
    }
}
