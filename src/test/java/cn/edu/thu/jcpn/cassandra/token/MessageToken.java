package cn.edu.thu.jcpn.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/1/2.
 */
public class MessageToken extends IToken {

    private int rid;
    private String key;
    private String value;
    private TokenType type;

    public MessageToken(int rid, String key, String value, TokenType type) {
        super();
        this.rid = rid;
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public int getRid() {
        return rid;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public TokenType getType() {
        return type;
    }
}
