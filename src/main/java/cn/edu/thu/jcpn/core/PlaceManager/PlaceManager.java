package cn.edu.thu.jcpn.core.PlaceManager;

import cn.edu.thu.jcpn.core.PlaceManager.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.place.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.transition.runtime.RuntimeTransition;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.edu.thu.jcpn.core.PlaceManager.PlaceManager.TokenType.*;

/**
 * Created by leven on 2017/1/5.
 */
public class PlaceManager {

    private Map<INode, Map<Integer, RuntimePlace>> nodePidPlaces;

    private Map<INode, Map<Integer, IPlaceMonitor>> nodePidMonitors;

    public void addTokens(INode owner, int pid, List<IToken> tokens, RuntimeTransition transition) {
        RuntimePlace place = nodePidPlaces.get(owner).get(pid);

        place.addTokens(tokens);

        IPlaceMonitor monitor = nodePidMonitors.get(owner).get(pid);
        if (null != monitor) {
            monitor.reportAfterTokensAdded(owner, place.getId(), place.getName(), tokens, transition.getOwner(),
                    transition.getId(), transition.getName(), place.getTimeoutTokens(), place.getTestedTokens(),
                    place.getNewlyTokens(), place.getFutureTokens());

            Map<TokenType, Map<INode, Collection<IToken>>> pidAllTokens = getPidAllTokens(pid);
            monitor.reportAfterTokensAdded(owner, place.getId(), place.getName(), tokens, pidAllTokens.get(TIMEOUT),
                    pidAllTokens.get(TESTED), pidAllTokens.get(NEWLY), pidAllTokens.get(FUTURE));
        }
    }

    public void removeTokens(INode owner, int pid, List<IToken> tokens, RuntimeTransition transition) {
        RuntimePlace place = nodePidPlaces.get(owner).get(pid);

        place.addTokens(tokens);

        IPlaceMonitor monitor = nodePidMonitors.get(owner).get(pid);
        if (null != monitor) {
            monitor.reportAfterTokensAdded(owner, place.getId(), place.getName(), tokens, transition.getOwner(),
                    transition.getId(), transition.getName(), place.getTimeoutTokens(), place.getTestedTokens(),
                    place.getNewlyTokens(), place.getFutureTokens());

            Map<TokenType, Map<INode, Collection<IToken>>> pidAllTokens = getPidAllTokens(pid);
            monitor.reportAfterTokensAdded(owner, place.getId(), place.getName(), tokens, pidAllTokens.get(TIMEOUT),
                    pidAllTokens.get(TESTED), pidAllTokens.get(NEWLY), pidAllTokens.get(FUTURE));
        }
    }

    private Map<TokenType, Map<INode, Collection<IToken>>> getPidAllTokens(int pid) {
        Map<TokenType, Map<INode, Collection<IToken>>> res = new HashMap<>();
        Map<INode, Collection<IToken>> timeout = new HashMap<>();
        Map<INode, Collection<IToken>> tested = new HashMap<>();
        Map<INode, Collection<IToken>> newly = new HashMap<>();
        Map<INode, Collection<IToken>> future = new HashMap<>();
        nodePidPlaces.forEach((node, pidPlaces) -> {
            RuntimePlace runtimePlace = pidPlaces.get(pid);
            timeout.put(node, runtimePlace.getTimeoutTokens());
            tested.put(node, runtimePlace.getTestedTokens());
            newly.put(node, runtimePlace.getNewlyTokens());
            future.put(node, runtimePlace.getFutureTokens());
        });
        return res;
    }

    enum TokenType {
        TIMEOUT, TESTED, NEWLY, FUTURE
    }
}
