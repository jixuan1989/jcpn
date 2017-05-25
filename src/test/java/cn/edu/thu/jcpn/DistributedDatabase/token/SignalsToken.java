package cn.edu.thu.jcpn.DistributedDatabase.token;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.runtime.tokens.UnitToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by leven on 2016/12/19.
 */
public class SignalsToken extends IToken {

    private List<IToken> signals;

    public SignalsToken() {
        super();
        this.signals = new ArrayList<>();
    }

    public SignalsToken(INode owner) {
        this();
        this.owner = owner;
    }

    public SignalsToken(INode owner, List<INode> tos) {
        this();
        tos.stream().filter(to -> to != owner).forEach(to -> signals.add(new UnitToken(null, owner, to)));
    }

    public SignalsToken(SignalsToken other) {
        this();
        other.signals.forEach(signal -> this.signals.add(new UnitToken(signal.getFrom(), signal.getOwner(), signal.getTo())));
    }

    public List<IToken> getSignals() {
        return signals;
    }

    public void add(IToken signal) {
        if (!signals.contains(signal)) signals.add(signal);
    }

}
