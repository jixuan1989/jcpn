package cn.edu.thu.jcpn.DistributedDatabase.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/5/6.
 */
public class KeySignalToken extends IToken {

    private int key;
    private int signal;

    public KeySignalToken(int key, int signal) {
        super();
        this.key = key;
        this.signal = signal;
    }

    public int getKey() {
        return key;
    }

    public int getSignal() {
        return signal;
    }
}
