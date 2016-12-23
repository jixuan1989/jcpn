package cn.edu.thu.jcpn.elements.token;

import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.ITarget;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2016/12/19.
 */
public class MessageToken extends IToken {

    private int message;

    public MessageToken(int message) {
        this.message = message;
    }

    public MessageToken(IOwner owner, ITarget target, int message) {
        super(owner, target);
        this.message = message;
    }

    public int getMessage() {
        return message;
    }

    public void setMessage(int message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "MessageToken{" +
                "message=" + message +
                '}';
    }
}
