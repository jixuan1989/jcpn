package cn.edu.thu.jcpn.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/1/2.
 */
public class RequestToken extends IToken {

    private int id;
    private String key;
    private String value;
    private long version;
    private int consistency;

    private static int count = 0;

    public RequestToken(String key, String value, int consistency, long absoluteTime) {
        super(0);
        this.time = absoluteTime;
        this.id = count++;
        this.key = key;
        this.value = value;
        this.consistency = consistency;
        this.version = this.time;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
