package cn.edu.thu.jcpn.core.transition.condition;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.HashMap;


/**
 * InputToken is made up by tokens which are extract from the place of this individual server.
 * So
 * Created by leven on 2016/12/7.
 */
public class InputToken extends HashMap<Integer, IToken> {

    public InputToken() {
        super();
    }

    public InputToken(InputToken inputToken) {
        super(inputToken);
    }

    /**
     * note: if return null, it means this tokenSet does not contain (to, pid)'s tokens.
     *
     * @param pid
     * @return
     */
    public IToken get(int pid) {
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

    public InputToken merge(InputToken inputToken) {
        super.putAll(inputToken);
        return this;
    }

    public void removeToken(int pid) {
        super.remove(pid);
    }

    /**
     * warning: if a token (which belongs to a place that in the placeSet) is not in the tokenSet,
     * you will get a null, which means an error occurred.
     *
     * @param partition
     * @return null or a InputToken.
     */
    public InputToken getByPids(PlacePartition partition) {
        InputToken res = new InputToken();
        for (int pid : partition) {
            IToken value = super.get(pid);
            if (null == value) {
                return null;
            }
            res.put(pid, value);
        }
        return res;
    }
}
