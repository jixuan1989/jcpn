package cn.edu.thu.jcpn.core.monitor;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.Collection;

/**
 * Created by leven on 2016/12/24.
 */
public interface ILookupMonitor {

    void reportWhenProperityRead(INode owner, int lookupTableId, String lookupTableName, String key, String value, int transitionId, String transitionName);

    void reportWhenProperityAdded(INode owner, int lookupTableId, String lookupTableName, String key, String value, int transitionId, String transitionName);

    void reportWhenProperityUpdated(INode owner, int lookupTableId, String lookupTableName, String key, String value, int transitionId, String transitionName);

    void reportWhenProperityRemoved(INode owner, int lookupTableId, String lookupTableName, String key, String value, int transitionId, String transitionName);
}
