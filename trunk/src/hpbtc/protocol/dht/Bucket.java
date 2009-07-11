/*
 * Created on 17.10.2008
 */
package hpbtc.protocol.dht;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class Bucket {

    private List<DHTNode> nodes = new LinkedList<DHTNode>();
    private byte[] min;
    private byte[] max;
    private TimerTask refreshTask;

    Bucket() {
        min = new byte[20];
        max = new byte[20];
    }

    Bucket(final byte[] min, final byte[] max) {
        this.min = min;
        this.max = max;
    }

    public boolean cancelRefreshTask() {
        if (refreshTask != null) {
            return refreshTask.cancel();
        } else {
            return true;
        }
    }

    public void setRefreshTask(final TimerTask refreshTask) {
        this.refreshTask = refreshTask;
    }

    byte[] getMin() {
        return min;
    }

    byte[] getMax() {
        return max;
    }

    DHTNode getNode(final byte[] nid) {
        Iterator<DHTNode> it = nodes.iterator();
        while (it.hasNext()) {
            DHTNode n = it.next();
            if (Arrays.equals(n.getId(), nid)) {
                return n;
            }
        }
        return null;
    }

    boolean insertNode(final DHTNode node) {
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
        return true;
    }

    List<DHTNode> getNodes() {
        return nodes;
    }

    List<String> getCompactNodes() {
        final List<String> sb = new LinkedList<String>();
        for (DHTNode n : nodes) {
            sb.add(n.getCompactNodeInfo());
        }
        return sb;
    }
}
