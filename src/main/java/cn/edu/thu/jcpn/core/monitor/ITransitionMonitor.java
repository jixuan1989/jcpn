package cn.edu.thu.jcpn.core.monitor;

import cn.edu.thu.jcpn.core.executor.transition.condition.ContainerPartition;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.executor.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;

import java.util.List;
import java.util.Map;

/**
 * Created by leven on 2016/12/24.
 */
public interface ITransitionMonitor {

    default void reportWhenFiring(long currentTime, INode owner, int transitionId, String transitionName, InputToken inputToken, OutputToken outputToken) {}

    default void reportWhenFiring(InputToken inputToken, Map<ContainerPartition, List<InputToken>> cache) {}

}
