package cn.edu.thu.jcpn.core.container.runtime;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.List;

/**
 * Created by leven on 2017/1/7.
 */
public interface IRuntimeContainer {

    int getId();

    String getName();

    INode getOwner();

    void addTokens(List<IToken> tokens);

    List<IToken> reassignTokens();

    void logStatus();
}
