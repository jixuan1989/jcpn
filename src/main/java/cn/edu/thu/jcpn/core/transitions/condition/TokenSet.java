package cn.edu.thu.jcpn.core.transitions.condition;

import cn.edu.thu.jcpn.core.places.runtime.IOwner;
import cn.edu.thu.jcpn.core.places.runtime.ITarget;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.HashMap;
import java.util.List;


/**
 * Created by leven on 2016/12/7.
 */
public class TokenSet extends HashMap<Integer, IToken> {

    private IOwner owner;
    private ITarget target;

    public TokenSet() {
        super();
    }

    public TokenSet(TokenSet tokenSet) {
        super();
        super.putAll(tokenSet);
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

    public IToken getByPid(int pid) {
        return super.get(pid);
    }

    /**
     * note: if the pid is already exist, recover the value.
     *
     * @param pid
     * @param token
     */
    public void addToken(int pid, IToken token) {
        super.put(pid, token);
    }

    public TokenSet addTokenSet(TokenSet tokenSet) {
        this.putAll(tokenSet);
        return this;
    }

    public void removeToken(int pid) {
        super.remove(pid);
    }

    /**
     * warning: if a token (which belongs to a place that in the placeSet) is not in the tokenSet,
     * you will get a null, which means an error occurred.
     *
     * @param placeSet
     * @return null or a TokenSet.
     */
    public TokenSet getByPids(PlaceSet placeSet) {
        TokenSet res = new TokenSet();
        for (int pid : placeSet) {
            IToken value = super.get(pid);
            if (null == value) {
                return null;
            }
            res.put(pid, value);
        }
        return res;
    }

    public static TokenSet combine(List<TokenSet> tokenSets) {
        TokenSet res = new TokenSet();
        tokenSets.forEach(tokenSet -> res.putAll(tokenSet));
        return res;
    }
}
