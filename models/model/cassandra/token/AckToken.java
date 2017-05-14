package model.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/1/2.
 */
public class AckToken extends IToken {

    private int rid;

    public AckToken(int rid) {
        super();
        this.rid = rid;
    }

    public int getRid() {
        return rid;
    }
}
