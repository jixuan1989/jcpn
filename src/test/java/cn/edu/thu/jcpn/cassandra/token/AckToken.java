package cn.edu.thu.jcpn.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/1/2.
 */
public class AckToken extends IToken {

    private int rid;

    public AckToken(int rid, long effective) {
        super();
        super.setTimeCost(effective);
        this.rid = rid;
    }

    public int getRid() {
        return rid;
    }
}
