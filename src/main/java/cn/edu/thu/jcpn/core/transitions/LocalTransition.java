package cn.edu.thu.jcpn.core.transitions;

public class LocalTransition extends Transition {

    public LocalTransition(int id, String name) {
        super(id, name);
        this.setType(TransitionType.LOCAL);
    }
}
