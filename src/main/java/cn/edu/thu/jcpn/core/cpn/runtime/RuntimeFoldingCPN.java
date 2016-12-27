package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.common.Pair;
import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.transition.Transition;
import cn.edu.thu.jcpn.core.transition.runtime.RuntimeTransition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType.SENDING;

public class RuntimeFoldingCPN {

    private static Logger logger = LogManager.getLogger();

    private CPN graph;
    private List<INode> owners;
    private Map<INode, RuntimeIndividualCPN> individualCPNs;
    private boolean compiled;

    /**
     * // set the maximal execution time of CPN global clock.
     * (because some CPN can execute forever, so we can disrupt the process after maximumExecutionTime.)
     */
    private GlobalClock globalClock;
    private long maximumExecutionTime;

    public RuntimeFoldingCPN(CPN graph, List<INode> owners) {
        this.graph = graph;
        this.owners = owners;
        this.individualCPNs = new HashMap<>();
        this.compiled = false;

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

        for (INode owner : owners) {
            RuntimeIndividualCPN individualCPN = new RuntimeIndividualCPN(owner, this);
            individualCPN.construct(places, transitions);
            individualCPNs.put(owner, individualCPN);
        }

        owners.forEach(owner -> globalClock.addAbsoluteTimepointForRunning(owner, 0L));

        compiled = true;
        return compiled;
    }

    public List<INode> getOwners() {
        return owners;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public long getMaximumExecutionTime() {
        return maximumExecutionTime;
    }

    public void setMaximumExecutionTime(long maximumExecutionTime) {
        this.maximumExecutionTime = maximumExecutionTime;
    }

    public void addMonitor(int pid, IPlaceMonitor monitor) {
        owners.forEach(owner -> addMonitor(owner, pid, monitor));
    }

    public void addMonitor(INode owner, int pid, IPlaceMonitor monitor) {
        if (!owners.contains(owner) || !graph.getPlaces().containsKey(pid)) return;

        individualCPNs.get(owner).addMonitor(pid, monitor);
    }

    public void addMonitor(int tid, ITransitionMonitor monitor) {
        owners.forEach(owner -> addMonitor(owner, tid, monitor));
    }

    public void addMonitor(INode owner, int tid, ITransitionMonitor monitor) {
        if (!owners.contains(owner) || !graph.getTransitions().containsKey(tid)) return;

        individualCPNs.get(owner).addMonitor(tid, monitor);
    }

    /**
     * get the individualCPN of the owner.
     *
     * note: if the method is called by a transition, it will get the to(remote) individual CPN.
     * and if the owner is a LocalAsTarget type, it means get the owner itself. So the owner 'from'
     * represents the owner of the transition.
     * If you will not pass a LocalAsTarget type of owner, you may just pass the 'null' to the second param.
     *
     * @param owner the owner of the individual CPN you want to get.
     * @return
     */
    public RuntimeIndividualCPN getIndividualCPN(INode owner) {
        //INode realOwner = (owner instanceof LocalAsTarget) ? from : owner;
        //return individualCPNs.get(realOwner);
        return individualCPNs.get(owner);
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

        Pair<GlobalClock.EventType, Map.Entry<Long, Map<INode, Object>>> nextEventTimeOwner = globalClock.timeElapse();
        GlobalClock.EventType eventType = nextEventTimeOwner.getLeft();
        Map.Entry<Long, Map<INode, Object>> timeOwner = nextEventTimeOwner.getRight();
        logStatus();
        globalClock.logStatus();
        if (eventType.equals(SENDING)) {
            logger.trace(() -> "will get next sending event..." + timeOwner);
            System.out.println("run sending events:");
            timeOwner.getValue().keySet().parallelStream().forEach(owner -> runACPNInstance(getIndividualCPN(owner)));
        } else {
            logger.trace(() -> "will get next running event..." + timeOwner);
            System.out.println("run running events:");
            timeOwner.getValue().keySet().parallelStream().forEach(owner -> runACPNInstance(getIndividualCPN(owner)));
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

    public void logStatus() {
        individualCPNs.values().forEach(RuntimeIndividualCPN::logStatus);
    }
}
