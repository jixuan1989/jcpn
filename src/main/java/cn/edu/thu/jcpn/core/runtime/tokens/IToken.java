package cn.edu.thu.jcpn.core.runtime.tokens;

/**
 * basic colset interface, all colsets must implement this interface.
 *
 * @author hxd
 */
public abstract class IToken {

    protected IOwner owner;
    protected ITarget target;

    protected long time;

    protected IToken() {

    }

    protected IToken(IOwner owner, ITarget target) {
        this.owner = owner;
        this.target = target;
    }

    public IOwner getOwner() {
        return owner;
    }

    public void setOwner(IOwner owner) {
        this.owner = owner;
    }

    public ITarget getTarget() {
        return target;
    }

    public void setTarget(ITarget target) {
        this.target = target;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
