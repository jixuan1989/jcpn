package model.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/1/2.
 */
public class CallBackToken extends IToken {

    private int rid;
    private int callback;

    public CallBackToken(int rid, int callback) {
        super();
        this.rid = rid;
        this.callback = callback;
    }

    public int getRid() {
        return rid;
    }

    public int getCallback() {
        return callback;
    }

    public void ack() {
        --callback;
    }
}
