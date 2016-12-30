package cn.edu.thu.jcpn.core.monitor;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.Collection;
import java.util.Map;

/**
 * Created by leven on 2016/12/24.
 */
public interface ILookupMonitor {

    default void reportWhenProperityRead(INode owner, int lookupTableId, String lookupTableName, String key, String value, int transitionId, String transitionName) {}

    default void reportWhenProperityAdded(INode owner, int lookupTableId, String lookupTableName, String key, String value, int transitionId, String transitionName) {}

    default void reportWhenProperityUpdated(INode owner, int lookupTableId, String lookupTableName, String key, String value, int transitionId, String transitionName) {}

    default void reportWhenProperityRemoved(INode owner, int lookupTableId, String lookupTableName, String key, String value, int transitionId, String transitionName) {}

    default void reportWhenTokenConsumed(
            INode owner, int placeId, String placeName, IToken consumed, int transitionId, String transitionName,
            Map<INode, Collection<IToken>> tested,
            Map<INode, Collection<IToken>> newly,
            Map<INode, Collection<IToken>> future) {}

    default void reportWhenTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens, int transitionId, String transitionName,
                               Map<INode, Collection<IToken>> tested,
                               Map<INode, Collection<IToken>> newly,
                               Map<INode, Collection<IToken>> future) {}
}
