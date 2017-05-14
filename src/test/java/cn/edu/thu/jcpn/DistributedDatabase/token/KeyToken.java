package cn.edu.thu.jcpn.DistributedDatabase.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/5/6.
 */
public class KeyToken extends IToken {

    private int key;

    public KeyToken(int key) {
        super();
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}
