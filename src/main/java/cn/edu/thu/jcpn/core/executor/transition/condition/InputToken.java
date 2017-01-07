package cn.edu.thu.jcpn.core.executor.transition.condition;

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
     * @param cid
     * @return
     */
    public IToken get(int cid) {
        return super.get(cid);
    }

    /**
     * note: if the cid is already exist, execute the value.
     *
     * @param cid
     * @param token
     */
    public void addToken(int cid, IToken token) {
        super.put(cid, token);
    }

    public InputToken merge(InputToken inputToken) {
        super.putAll(inputToken);
        return this;
    }

    public void removeToken(int cid) {
        super.remove(cid);
    }

    /**
     * warning: if a token (which belongs to a place that in the placeSet) is not in the tokenSet,
     * you will get a null, which means an error occurred.
     *
     * @param partition
     * @return null or a InputToken.
     */
    public InputToken getByCids(ContainerPartition partition) {
        InputToken res = new InputToken();
        for (int cid : partition) {
            IToken value = super.get(cid);
            if (null == value) {
                return null;
            }
            res.put(cid, value);
        }
        return res;
    }
}
