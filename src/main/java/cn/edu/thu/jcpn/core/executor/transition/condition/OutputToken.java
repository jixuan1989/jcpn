package cn.edu.thu.jcpn.core.executor.transition.condition;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <to, pid, tokens>, output to pid place of the to server with tokens.
 *
 * Created by leven on 2016/12/7.
 */
public class OutputToken extends HashMap<INode, Map<Integer, List<IToken>>> {

    public OutputToken() {
        super();
    }

    /**
     * note: if return null, it means this tokenSet does not contain (to, pid)'s tokens.
     *
     * @param target
     * @param cid
     * @return
     */
    public List<IToken> get(INode target, int cid) {
        return super.getOrDefault(target, new HashMap<>()).get(cid);
    }

    /**
     * note: if the pid is already exist, execute the value.
     *
     * @param target
     * @param cid
     * @param token
     */
    public void addToken(INode target, int cid, IToken token) {
        super.computeIfAbsent(target, obj -> new HashMap<>()).
                computeIfAbsent(cid, obj -> new ArrayList<>()).add(token);
    }
}
