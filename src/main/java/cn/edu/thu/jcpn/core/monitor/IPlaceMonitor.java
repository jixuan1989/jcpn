package cn.edu.thu.jcpn.core.monitor;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.Collection;

/**
 * Created by leven on 2016/12/24.
 */
public interface IPlaceMonitor {

    void reportWhenTokenConsumed(INode owner, int placeId, String placeName, IToken consumed, int transitionId, String transitionName,
                                 Collection<IToken> tested, Collection<IToken> newly, Collection<IToken> future);

    void reportWhenTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens, INode from, int transitionId, String transitionName,
                               Collection<IToken> tested, Collection<IToken> newly, Collection<IToken> future);
}
