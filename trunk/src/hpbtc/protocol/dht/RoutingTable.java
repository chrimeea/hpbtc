/*
 * Created on 17.10.2008
 */
package hpbtc.protocol.dht;

import hpbtc.util.ByteStringComparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class RoutingTable {

    private static ByteStringComparator bsc = new ByteStringComparator();
    private List<Bucket> table;
    private byte[] nodeID;

    public RoutingTable() {
        table = new LinkedList<Bucket>();
        table.add(new Bucket());
        nodeID = new byte[20];
        Random r = new Random();
        r.nextBytes(nodeID);
    }

    public void insertNode(Node node) {
        int s = table.size();
        int pos = s / 2;
        Bucket b = table.get(pos);
        byte[] nid = node.getId();
        int r = bsc.compare(b.getMax(), nid);
        while (r >= 0 || bsc.compare(b.getMin(), nid) < 0) {
            if (r < 0) {
                pos = pos / 2;
            } else {
                pos = (pos + s) / 2;
            }
            b = table.get(pos);
            r = bsc.compare(b.getMax(), nid);
        }
        if (!b.insertNode(node) && shouldContain(b, nodeID)) {
            Bucket[] t = new Bucket[2];
            byte[] d = DHTUtil.divideByTwo(b.getMax());
            t[0] = new Bucket(b.getMin(), d);
            t[1] = new Bucket(d, b.getMax());
            for (Node i : b.getNodes()) {
                if (bsc.compare(i.getId(), d) < 0) {
                    t[0].insertNode(i);
                } else {
                    t[1].insertNode(i);
                }
            }
            if (shouldContain(t[0], nid)) {
                t[0].insertNode(node);
            } else {
                t[1].insertNode(node);
            }
            table.remove(pos);
            table.add(pos, t[1]);
            table.add(pos, t[0]);

        }
    }

    private static boolean shouldContain(Bucket b, byte[] n) {
        return bsc.compare(b.getMin(), n) <= 0 && bsc.compare(b.getMax(), n) > 0;
    }
}