package cn.edu.thu.jcpn.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by hxd on 17/1/7.
 */
public class SignalToken extends IToken {

    private INode signal;

    public SignalToken(INode signal) {
        super();
        this.signal = signal;
    }

    public INode getSignal() {
        return signal;
    }
}
