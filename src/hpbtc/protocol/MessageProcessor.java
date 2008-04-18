/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hpbtc.protocol;

import hpbtc.protocol.BitfieldMessage;
import hpbtc.protocol.CancelMessage;
import hpbtc.protocol.ChokeMessage;
import hpbtc.protocol.HandshakeMessage;
import hpbtc.protocol.HaveMessage;
import hpbtc.protocol.IdleMessage;
import hpbtc.protocol.InterestedMessage;
import hpbtc.protocol.NotInterestedMessage;
import hpbtc.protocol.PIDMessage;
import hpbtc.protocol.PieceMessage;
import hpbtc.protocol.ProtocolMessage;
import hpbtc.protocol.RequestMessage;
import hpbtc.protocol.UnchokeMessage;

/**
 *
 * @author Cristian Mocanu
 */
public interface MessageProcessor {

    void process(BitfieldMessage message);

    void process(CancelMessage message);

    void process(ChokeMessage message);

    void process(HandshakeMessage message);

    void process(HaveMessage message);

    void process(IdleMessage message);

    void process(InterestedMessage message);

    void process(NotInterestedMessage message);

    void process(PIDMessage message);

    void process(PieceMessage message);

    void process(RequestMessage message);

    void process(UnchokeMessage message);

}
