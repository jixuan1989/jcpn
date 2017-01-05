package cn.edu.thu.jcpn.core.PlaceManager.monitor;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.Collection;
import java.util.Map;

/**
 * Created by leven on 2016/12/24.
 */
public interface IPlaceMonitor {

    default void reportAfterTokenConsumed(INode owner, int placeId, String placeName, IToken consumed, int transitionId, String transitionName,
                                          Collection<IToken> tested, Collection<IToken> newly, Collection<IToken> future) {
    }

    default void reportAfterTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens,
                                        INode from, int transitionId, String transitionName, Collection<IToken> timeout,
                                        Collection<IToken> tested, Collection<IToken> newly, Collection<IToken> future) {
    }

    default void reportAfterTokenConsumed(
            INode owner, int placeId, String placeName, IToken consumed, int transitionId, String transitionName,
            Map<INode, Collection<IToken>> tested,
            Map<INode, Collection<IToken>> newly,
            Map<INode, Collection<IToken>> future) {
    }

    default void reportAfterTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens,
                                        Map<INode, Collection<IToken>> timeout,
                                        Map<INode, Collection<IToken>> tested,
                                        Map<INode, Collection<IToken>> newly,
                                        Map<INode, Collection<IToken>> future) {
    }
}
