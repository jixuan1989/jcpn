package cn.edu.thu.jcpn.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/1/2.
 */
public class RequestToken extends IToken {

    private int id;
    private String key;
    private String value;
    private int consistency;

    private static int count = 0;

    public RequestToken(String key, String value, int consistency) {
        super();
        this.id = count++;
        this.key = key;
        this.value = value;
        this.consistency = consistency;
    }

    public int getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public int getConsistency() {
        return consistency;
    }
}
