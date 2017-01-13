package model.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by leven on 2017/1/6.
 */
public class RequestToken extends IToken {

    private static AtomicInteger count = new AtomicInteger(0);

    private int id;
    private String key;
    private String value;
    private long version;
    private int consistency;

    public RequestToken(String key, String value, int consistency) {
        super();
        this.id = count.incrementAndGet();
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
