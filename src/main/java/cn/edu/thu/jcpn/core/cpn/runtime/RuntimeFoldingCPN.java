package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.common.Pair;
import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.LocalAsTarget;
import cn.edu.thu.jcpn.core.transition.Transition;
import cn.edu.thu.jcpn.core.transition.runtime.RuntimeTransition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType.SENDING;

public class RuntimeFoldingCPN {

    private static Logger logger = LogManager.getLogger();

    private CPN graph;
    private List<IOwner> owners;
    private Map<IOwner, RuntimeIndividualCPN> individualCPNs;

    /**
     * // set the maximal execution time of CPN global clock.
     * (because some CPN can execute forever, so we can disrupt the process after maximumExecutionTime.)
     */
    private GlobalClock globalClock;
    private long maximumExecutionTime;

    private boolean compiled = false;

    public RuntimeFoldingCPN(CPN graph, List<IOwner> owners) {
        this.graph = graph;
        this.owners = owners;
        this.individualCPNs = new HashMap<>();
        this.globalClock = GlobalClock.getInstance();
        this.maximumExecutionTime = Long.MAX_VALUE;

        this.compile();
    }

    /**
     * unfold the CPN net. Then, compile its foldingCPNInstance if needed.
     * If the method compiles the folding cpn, it will register running events at time 0L for
     * all the individual cpn instance, i.e., all the servers.
     *
     * @return
     */
    public boolean compile() {
        if (compiled) return true;

        Collection<Place> places = graph.getPlaces().values();
        Collection<Transition> transitions = graph.getTransitions().values();

        for (IOwner owner : owners) {
            RuntimeIndividualCPN individualCPN = new RuntimeIndividualCPN(owner, this);
            individualCPN.construct(places, transitions);
            individualCPNs.put(owner, individualCPN);
        }

        owners.forEach(owner -> globalClock.addAbsoluteTimepointForRunning(owner, 0L));

        compiled = true;
        return compiled;
    }

    public List<IOwner> getOwners() {
        return owners;
    }

    public boolean isCompiled() {
        return compiled;
    }

    /**
     * get the individualCPN of the owner.
     *
     * note: if the method is called by a transition, it will get the target(remote) individual CPN.
     * and if the owner is a LocalAsTarget type, it means get the owner itself. So the owner 'from'
     * represents the owner of the transition.
     * If you will not pass a LocalAsTarget type of owner, you may just pass the 'null' to the second param.
     *
     * @param owner the owner of the individual CPN you want to get.
     * @param from the real owner if the first param is LocalAsTarget Type.
     * @return
     */
    public RuntimeIndividualCPN getIndividualCPN(IOwner owner, IOwner from) {
        IOwner realOwner = (owner instanceof LocalAsTarget) ? from : owner;
        return individualCPNs.get(realOwner);
    }


    /**
     * if the global clock is less than the expected maximum execution Time, we say that it is not timeout,
     * otherwise it is timeout.
     */
    private boolean isTimeout() {
        return globalClock.getTime() >= maximumExecutionTime;
    }

    public boolean hasNextTime() {
        return !isTimeout() && globalClock.hasNextTime();
    }

    /**
     * @return whether the sending queue has events to do.
     */
    public boolean hasNextSendingTime() {
        return !isTimeout() && globalClock.hasNextSendingTime();
    }

    /**
     * @return whether the running queue has events to do.
     */
    public boolean hasNextRunningTime() {
        return !isTimeout() && globalClock.hasNextRunningTime();
    }

    public boolean nextRound() {
        if (!isTimeout() && !globalClock.hasNextTime()) {
            return false;
        }

        Pair<GlobalClock.EventType, Map.Entry<Long, Map<IOwner, Object>>> nextEventTimeOwner = globalClock.timeElapse();
        GlobalClock.EventType eventType = nextEventTimeOwner.getLeft();
        Map.Entry<Long, Map<IOwner, Object>> timeOwner = nextEventTimeOwner.getRight();
        if (eventType.equals(SENDING)) {
            logger.trace(() -> "will get next sending event..." + timeOwner);
            System.out.println("run sending events:");
            timeOwner.getValue().keySet().parallelStream().forEach(owner -> runACPNInstance(getIndividualCPN(owner, null)));
        } else {
            logger.trace(() -> "will get next running event..." + timeOwner);
            System.out.println("run running events:");
            timeOwner.getValue().keySet().parallelStream().forEach(owner -> runACPNInstance(getIndividualCPN(owner, null)));
        }

        logStatus();
        return true;
    }

    private void runACPNInstance(RuntimeIndividualCPN individualCPN) {
        individualCPN.notifyTransitions();
        while (individualCPN.hasEnableTransitions()) {
            RuntimeTransition transition = individualCPN.randomEnable();
            individualCPN.firing(transition);
        }
    }

    public void logStatus() {
        individualCPNs.values().forEach(RuntimeIndividualCPN::logStatus);
    }
}
