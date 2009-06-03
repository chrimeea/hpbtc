/*
 * Created on 17.10.2008
 */
package hpbtc.protocol.dht;

import hpbtc.util.ByteStringComparator;
import hpbtc.util.DHTUtil;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class RoutingTable {

    private static final ByteStringComparator bsc = new ByteStringComparator();
    private List<Bucket> table;
    private byte[] nodeID;
    private Timer timer;

    RoutingTable(final Timer timer) {
        this.timer = timer;
        table = new LinkedList<Bucket>();
        table.add(new Bucket());
        nodeID = new byte[20];
        Random r = new Random();
        r.nextBytes(nodeID);
    }

    byte[] getNodeID() {
        return nodeID;
    }

    Bucket findBucket(final byte[] nid) {
        return findBucket(nid, 0, table.size());
    }

    private Bucket findBucket(final byte[] nid, final int minpos, final int maxpos) {
        int pos = minpos + (maxpos - minpos) / 2;
        Bucket b = table.get(pos);
        if (bsc.compare(b.getMin(), nid) > 0) {
            return findBucket(nid, minpos, pos - 1);
        } else if (bsc.compare(b.getMax(), nid) <= 0) {
            return findBucket(nid, pos + 1, maxpos);
        } else {
            return b;
        }
    }

    private void insertNode(final DHTNode node, byte[] infohash) {
        byte[] nid = node.getId();
        Bucket b = findBucket(nid);
        if (!b.insertNode(node)) {
            byte[] d = DHTUtil.divideByTwo(b.getMax());
            Bucket b1 = new Bucket(b.getMin(), d);
            Bucket b2 = new Bucket(d, b.getMax());
            for (DHTNode i : b.getNodes()) {
                if (bsc.compare(i.getId(), d) < 0) {
                    b1.insertNode(i);
                } else {
                    b2.insertNode(i);
                }
            }
            if (bsc.compare(b1.getMax(), nid) < 0) {
                b1.insertNode(node);
            } else {
                b2.insertNode(node);
            }
            int pos = Collections.binarySearch(table, b, new Comparator<Bucket>() {

                public int compare(Bucket one, Bucket two) {
                    return bsc.compare(one.getMin(), two.getMin());
                }
            });
            table.remove(pos);
            table.add(pos, b2);
            table.add(pos, b1);
        }
    }
}