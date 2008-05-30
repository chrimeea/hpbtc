package hpbtc.processor;

import hpbtc.protocol.torrent.Peer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu
 */
public class PieceRepository {

    Map<byte[], Map<Long, List<Peer>>> pieces;
    
    public PieceRepository() {
        pieces = new HashMap<byte[], Map<Long, List<Peer>>>();
    }

    /*
    private boolean checkHash(ByteBuffer bb, byte[] hash) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        bb.rewind();
        md.update(bb);
        return Arrays.equals(md.digest(), hash);
    }
     */

}
