package cn.edu.thu.jcpn.core.executor;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;

/**
 * Created by leven on 2017/1/5.
 */
public interface IRuntimeExecutor {

    int getId();

    String getName();

    INode getOwner();
}
