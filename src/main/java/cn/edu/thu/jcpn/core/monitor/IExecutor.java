package cn.edu.thu.jcpn.core.monitor;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;

/**
 * Created by leven on 2017/1/5.
 */
public interface IExecutor {

    int getId();

    String getName();

    INode getOwner();
}
