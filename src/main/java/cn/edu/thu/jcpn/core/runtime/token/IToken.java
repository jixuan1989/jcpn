package cn.edu.thu.jcpn.core.runtime.token;

/**
 * basic colset interface, all colsets must implement this interface.
 *
 * @author hxd
 */
public abstract class IToken {

    protected long time;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

}
