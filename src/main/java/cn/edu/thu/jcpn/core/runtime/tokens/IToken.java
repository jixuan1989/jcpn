package cn.edu.thu.jcpn.core.runtime.tokens;

import cn.edu.thu.jcpn.core.runtime.GlobalClock;

/**
 * basic token interface, all tokens must implement this interface.
 *
 * @author hxd
 */
public abstract class IToken {

    protected INode from;

    protected INode owner;

    /**
     * If the token is stored in a place whose type is COMMUNICATING, then it can not be null.
     */
    protected INode to;

    protected long time;

    private static GlobalClock globalClock = GlobalClock.getInstance();

    protected IToken() {
    }

    protected IToken(INode from, INode owner, INode to) {
        this.from = from;
        this.owner = owner;
        this.to = to;
    }

    public INode getFrom() {
        return from;
    }

    public void setFrom(INode from) {
        this.from = from;
    }

    public INode getOwner() {
        return owner;
    }

    public void setOwner(INode owner) {
        this.owner = owner;
    }

    public INode getTo() {
        return to;
    }

    public void setTo(INode to) {
        this.to = to;
    }

    public long getTime() {
        return time;
    }

//    public void setTime(long time) {
//        this.time = time;
//    }

    public void setTimeCost(long timeCost) {
        this.time = globalClock.getTime() + timeCost;
    }
}
