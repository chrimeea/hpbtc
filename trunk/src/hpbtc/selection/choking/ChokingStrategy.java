package hpbtc.selection.choking;

import hpbtc.download.DownloadItem;
import hpbtc.peer.LightPeer;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author chris
 *
 */
public abstract class ChokingStrategy implements Serializable {
    
    public static final int UPLOAD_CAP = 3;
    
    /**
     * @param item
     * @return
     */
    public List<LightPeer> select(DownloadItem item) {
        List<LightPeer> lp = item.getPeers();
        Collections.shuffle(lp);
        for (int i = 0; i < UPLOAD_CAP; i++) {
            LightPeer p = select(lp);
            if (p != null) {
                p.setChoked(false);
            } else {
                break;
            }
        }
        return lp;
    }
    
    /**
     * @param lp
     * @return
     */
    public abstract LightPeer select(List<LightPeer> lp);
}
