package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.common.Triple;
import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.PlaceManager.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.recoverer.Recoverer;
import cn.edu.thu.jcpn.core.transition.Transition;
import cn.edu.thu.jcpn.core.transition.runtime.RuntimeTransition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RuntimeFoldingCPN {

    private static Logger logger = LogManager.getLogger();

    private CPN graph;
    private List<INode> owners;
    private Map<INode, RuntimeIndividualCPN> individualCPNs;
    private boolean compiled;

    private GlobalClock globalClock;
    /**
     * // set the maximal execution time of CPN global clock.
     * (because some CPN can execute forever, so we can disrupt the process after maximumExecutionTime.)
     */
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
        Collection<Recoverer> timeoutTransitions = graph.getRecoverers().values();

        for (INode owner : owners) {
            RuntimeIndividualCPN individualCPN = new RuntimeIndividualCPN(owner, this);
            individualCPN.construct(places, transitions, timeoutTransitions);
            individualCPNs.put(owner, individualCPN);
        }

        owners.forEach(owner -> globalClock.addAbsoluteTimePointForRemoteHandle(owner, 0L));

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
     * @param owner the owner of the individual CPN you want to get.
     * @return
     */
    public RuntimeIndividualCPN getIndividualCPN(INode owner) {
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

        globalClock.logStatus();

        Triple<EventType, Long, Set<INode>> nextEventTimeNodes = globalClock.timeElapse();
        if (null == nextEventTimeNodes) {
            return false;
        }

        EventType eventType = nextEventTimeNodes.getLeft();
        Set<INode> nodes = nextEventTimeNodes.getRight();

        logStatus();

        logger.trace(() -> String.format("Run handling %s events... %s", eventType.toString(), nodes));
        System.out.println(String.format("Run handling %s events... %s", eventType.toString(), nodes));
        nodes.parallelStream().forEach(node -> runACPNInstance(getIndividualCPN(node)));
        return true;
    }

    private void runACPNInstance(RuntimeIndividualCPN individualCPN) {
        individualCPN.neatenPlaces();
        if (individualCPN.hasEnableRecoverers()) {
            individualCPN.fireAllRecoverers();
            individualCPN.neatenPlaces();
        }

        individualCPN.notifyTransitions();
        while (individualCPN.hasEnableTransitions()) {
            RuntimeTransition transition = individualCPN.randomEnable();
            individualCPN.fire(transition);
        }
    }

    public void logStatus() {
        individualCPNs.values().forEach(RuntimeIndividualCPN::logStatus);
    }
}
