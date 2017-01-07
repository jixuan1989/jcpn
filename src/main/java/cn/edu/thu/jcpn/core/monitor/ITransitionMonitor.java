package cn.edu.thu.jcpn.core.monitor;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.executor.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;

/**
 * Created by leven on 2016/12/24.
 */
public interface ITransitionMonitor {

    void reportWhenFiring(INode owner, int transitionId, String transitionName, InputToken inputToken, OutputToken outputToken);
}
