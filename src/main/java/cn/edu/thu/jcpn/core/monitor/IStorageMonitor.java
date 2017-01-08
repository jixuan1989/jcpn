package cn.edu.thu.jcpn.core.monitor;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

/**
 * Created by leven on 2017/1/8.
 */
public interface IStorageMonitor {

    void reportAfterTokensAdded(long time, INode owner, String storageName, IToken token);
}
