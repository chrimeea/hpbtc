/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hpbtc.protocol.message;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.CancelMessage;
import hpbtc.protocol.message.ChokeMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.IdleMessage;
import hpbtc.protocol.message.InterestedMessage;
import hpbtc.protocol.message.NotInterestedMessage;
import hpbtc.protocol.message.PIDMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.ProtocolMessage;
import hpbtc.protocol.message.RequestMessage;
import hpbtc.protocol.message.UnchokeMessage;

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
