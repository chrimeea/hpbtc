/*
 * Created on Jan 26, 2006
 *
 */
package hpbtc.client.selection.peer;

import hpbtc.client.LightPeer;
import hpbtc.client.piece.LightPiece;

import java.io.Serializable;
import java.util.List;

/**
 * @author chris
 *
 */
public abstract class PeerSelectionStrategy implements Serializable {

    /**
     * @param p
     * @return
     */
    public abstract List<LightPeer> select(LightPiece p);
}
