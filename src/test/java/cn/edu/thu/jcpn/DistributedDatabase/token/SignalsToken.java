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

    public SignalsToken(INode owner, List<INode> tos) {
        super();
        this.signals = new CopyOnWriteArrayList<>();
        tos.stream().filter(to -> to != owner).forEach(to -> signals.add(new UnitToken(null, owner, to)));
    }

    public SignalsToken(SignalsToken other) {
        super();
        Collections.copy(this.signals, other.signals);
    }

    public List<IToken> getSignals() {
        return signals;
    }

    public void add(IToken signal) {
        if (!signals.contains(signal)) signals.add(signal);
    }

}
