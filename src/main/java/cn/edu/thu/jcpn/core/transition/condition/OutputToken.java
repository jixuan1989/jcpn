package cn.edu.thu.jcpn.core.transition.condition;

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
     * @param pid
     * @return
     */
    public List<IToken> get(INode target, int pid) {
        return super.getOrDefault(target, new HashMap<>()).get(pid);
    }

    /**
     * note: if the pid is already exist, execute the value.
     *
     * @param target
     * @param pid
     * @param token
     */
    public void addToken(INode target, int pid, IToken token) {
        super.computeIfAbsent(target, obj -> new HashMap<>()).
                computeIfAbsent(pid, obj -> new ArrayList<>()).add(token);
    }

    public OutputToken addTokenSet(OutputToken otherOutput) {
        otherOutput.forEach((target, pidTokensB) -> {
            // get the Map<pid, tokens> from the tokenSet(host) which is accepting other tokenSet.
            Map<Integer, List<IToken>> pidTokensA = super.computeIfAbsent(target, obj -> new HashMap<>());

            // the tokens from outside tokenSet is going to be added in the pid of the host.
            pidTokensB.forEach((pid, tokens) -> pidTokensA.computeIfAbsent(pid, obj -> new ArrayList<>()).addAll(tokens));
        });

        return this;
    }

    public void removeToken(INode target, int pid, IToken token) {
        super.getOrDefault(target, new HashMap<>()).getOrDefault(pid, new ArrayList<>()).remove(token);
    }
}
