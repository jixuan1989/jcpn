package model.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/1/2.
 */
public class ResponseToken extends IToken {

    private int rid;
    private ResponseType type;

    public ResponseToken(int rid, ResponseType type) {
        super();
        this.rid = rid;
        this.type = type;
    }

    public int getRid() {
        return rid;
    }

    public ResponseType getType() {
        return type;
    }

    public enum ResponseType {
        SUCCESS, TIMEOUT
    }
}
