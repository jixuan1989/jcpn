package cn.edu.thu.jcpn.core.runtime;

import cn.edu.thu.jcpn.common.Pair;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeIndividualCPN;
import cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.transitions.runtime.RuntimeTransition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Map.Entry;

import static cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType.SENDING;

/**
 * auto mode:
 * <br>
 * <pre>
 * SimulationEngine engine=new SimulationEngine(foldCPN);
 * engine.compile();
 * while(engine.hasNextTime()){
 *  System.out.println("next time point:"+engine.getNextTimepoint());
 * 	engine.nextRound();
 * }
 * </pre>
 * <br>
 * manually mode:
 *
 * @author hxd
 */
public class AutoSimulator extends Simulator {

    private static Logger logger = LogManager.getLogger();

    public AutoSimulator(RuntimeFoldingCPN foldingCPN) {
        super(foldingCPN);
    }

    public boolean nextRound() {
        if (!isTimeout() && !clock.hasNextTime()) {
            return false;
        }

        Pair<EventType, Entry<Long, Map<IOwner, Object>>> nextEventTimeOwner = clock.timeElapse();
        EventType eventType = nextEventTimeOwner.getLeft();
        Entry<Long, Map<IOwner, Object>> timeOwner = nextEventTimeOwner.getRight();
        if (eventType.equals(SENDING)) {
            logger.trace(() -> "will get next sending event..." + timeOwner);
            timeOwner.getValue().keySet().parallelStream().forEach(owner -> runACPNInstance(foldingCPN.getIndividualCPN(owner)));
        } else {
            logger.trace(() -> "will get next running event..." + timeOwner);
            timeOwner.getValue().keySet().parallelStream().forEach(owner -> runACPNInstance(foldingCPN.getIndividualCPN(owner)));
        }
        return true;
    }

    private void runACPNInstance(RuntimeIndividualCPN individualCPN) {
        individualCPN.notifyTransitions();
        while (individualCPN.hasEnableTransitions()) {
            RuntimeTransition transition = individualCPN.randomEnable();
            individualCPN.firing(transition);
        }
    }
}
