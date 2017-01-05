package cn.edu.thu.jcpn.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/1/2.
 */
public class WriteToken extends IToken {

    private int rid;
    private String key;
    private String value;

    public WriteToken(int rid, String key, String value, long effective) {
        super();
        super.setTimeCost(effective);
        this.rid = rid;
        this.key = key;
        this.value = value;
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
}
