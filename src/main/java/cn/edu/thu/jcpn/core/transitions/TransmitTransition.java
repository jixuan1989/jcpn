package cn.edu.thu.jcpn.core.transitions;

/**
 * Currently, ConnectionTransition doest not support conditions like: token_of_IP.value==token_of_CP.value
 * only token_of_IP1.value==token_of_IP2.value or token_of_CP1.value==token_of_CP2.value is valid.
 */
public class TransmitTransition extends Transition {

    public TransmitTransition(int id, String name) {
        super(id, name);
        this.setType(TransitionType.TRANSMIT);
    }
}
