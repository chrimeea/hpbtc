package hpbtc.protocol.message;

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
