package cn.edu.thu.jcpn.core.container;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.List;

/**
 * Created by leven on 2017/1/10.
 */
public interface IOnFireListener {

    void modifyTokens(List<IToken> availableTokens);
}
