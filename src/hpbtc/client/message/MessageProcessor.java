/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hpbtc.client.message;

import hpbtc.client.message.BitfieldMessage;
import hpbtc.client.message.CancelMessage;
import hpbtc.client.message.ChokeMessage;
import hpbtc.client.message.HandshakeMessage;
import hpbtc.client.message.HaveMessage;
import hpbtc.client.message.IdleMessage;
import hpbtc.client.message.InterestedMessage;
import hpbtc.client.message.NotInterestedMessage;
import hpbtc.client.message.PIDMessage;
import hpbtc.client.message.PieceMessage;
import hpbtc.client.message.ProtocolMessage;
import hpbtc.client.message.RequestMessage;
import hpbtc.client.message.UnchokeMessage;

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
