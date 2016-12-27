package cn.edu.thu.jcpn.core.monitor;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by hxd on 2016/12/27.
 */
public interface IGlobalPlaceMonitor {
    void reportWhenTokensConsumed(
            INode owner, int placeId, String placeName, IToken consumed, int transitionId, String transitionName,
            Map<INode, Collection<IToken>> tested,
            Map<INode, Collection<IToken>> newly,
            Map<INode, Collection<IToken>> future);

    void reportWhenTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens, int transitionId, String transitionName,
            Map<INode, Collection<IToken>> tested,
            Map<INode, Collection<IToken>> newly,
            Map<INode, Collection<IToken>> future);
}
