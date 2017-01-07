package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.common.Triple;
import cn.edu.thu.jcpn.core.container.IContainer;
import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.container.Place;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.executor.recoverer.Recoverer;
import cn.edu.thu.jcpn.core.executor.transition.Transition;
import cn.edu.thu.jcpn.core.executor.transition.runtime.RuntimeTransition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RuntimeFoldingCPN {

    private static Logger logger = LogManager.getLogger();

    private String version;

    private Set<CPN> cpns;
    private List<INode> nodes;
    private Map<INode, RuntimeIndividualCPN> nodeIndividualCPNs;

    private GlobalClock globalClock;
    /**
     * set the maximal execution time of the foldingCPN.
     * (because some type of foldingCPN can execute forever,
     * maximumExecutionTime enable us to disrupt the process after that.)
     */
    private long maximumExecutionTime;

    public RuntimeFoldingCPN() {
        this.cpns = new HashSet<>();
        this.nodes = new ArrayList<>();
        this.nodeIndividualCPNs = new HashMap<>();

        this.globalClock = GlobalClock.getInstance();
        this.maximumExecutionTime = Long.MAX_VALUE;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Set<CPN> getCpns() {
        return cpns;
    }

    public List<INode> getNodes() {
        return nodes;
    }

    public Map<INode, RuntimeIndividualCPN> getNodeIndividualCPNs() {
        return nodeIndividualCPNs;
    }

    public long getMaximumExecutionTime() {
        return maximumExecutionTime;
    }

    public void setMaximumExecutionTime(long maximumExecutionTime) {
        this.maximumExecutionTime = maximumExecutionTime;
    }

    public void addMonitor(int pid, IPlaceMonitor monitor) {
        nodeIndividualCPNs.values().stream().filter(individualCPN -> individualCPN.getContainers().containsKey(pid)).
                forEach(individualCPN -> addMonitor(individualCPN.getOwner(), pid, monitor));
    }

    public void addMonitor(INode node, int pid, IPlaceMonitor monitor) {
        if (!nodeIndividualCPNs.containsKey(node) || !nodeIndividualCPNs.get(node).getContainers().containsKey(pid))
            return;

        nodeIndividualCPNs.get(node).addMonitor(pid, monitor);
    }

    public void addMonitor(int tid, ITransitionMonitor monitor) {
        nodeIndividualCPNs.values().stream().filter(individualCPN -> individualCPN.getTransitions().containsKey(tid)).
                forEach(individualCPN -> addMonitor(individualCPN.getOwner(), tid, monitor));
    }

    public void addMonitor(INode node, int tid, ITransitionMonitor monitor) {
        if (!nodeIndividualCPNs.containsKey(node) || !nodeIndividualCPNs.get(node).getTransitions().containsKey(tid))
            return;

        nodeIndividualCPNs.get(node).addMonitor(tid, monitor);
    }

    public void addCpn(CPN cpn, List<INode> nodes) {
        this.cpns.add(cpn);
        this.nodes.addAll(nodes);

        compile(cpn, nodes);
    }

    /**
     * unfold the CPN net. Then, compile its foldingCPNInstance if needed.
     * If the method compiles the folding cpn, it will register running events at time 0L for
     * all the individual cpn instance, i.e., all the servers.
     *
     * @return
     */
    private void compile(CPN cpn, List<INode> nodes) {
        Collection<IContainer> containers = cpn.getContainers().values();
        Collection<Transition> transitions = cpn.getTransitions().values();
        Collection<Recoverer> recoverers = cpn.getRecoverers().values();

        nodes.forEach(node -> {
            RuntimeIndividualCPN individualCPN = new RuntimeIndividualCPN(node, this);
            individualCPN.construct(containers, transitions, recoverers);
            nodeIndividualCPNs.put(node, individualCPN);

            globalClock.addAbsoluteTimePointForRemoteHandle(node, 0L);
        });
    }

    /**
     * get the individualCPN of the owner.
     *
     * @param owner the owner of the individual CPN you want to get.
     * @return
     */
    public RuntimeIndividualCPN getIndividualCPN(INode owner) {
        return nodeIndividualCPNs.get(owner);
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

        //globalClock.logStatus();

        Triple<EventType, Long, Set<INode>> nextEventTimeNodes = globalClock.timeElapse();
        if (null == nextEventTimeNodes) {
            return false;
        }

        EventType eventType = nextEventTimeNodes.getLeft();
        Set<INode> nodes = nextEventTimeNodes.getRight();

        //logStatus();

        logger.trace(() -> String.format("Run %s events... %s", eventType.toString(), nodes));
        //System.out.println(String.format("Run %s events... %s", eventType.toString(), nodes));
        nodes.forEach(node -> runACPNInstance(getIndividualCPN(node))); //parallelStream
        return true;
    }

    private void runACPNInstance(RuntimeIndividualCPN individualCPN) {
        individualCPN.neatenContainers();
        if (individualCPN.hasEnableRecoverers()) {
            individualCPN.fireAllRecoverers();
            individualCPN.neatenContainers();
        }

        individualCPN.notifyTransitions();
        while (individualCPN.hasEnableTransitions()) {
            RuntimeTransition transition = individualCPN.randomEnable();
            individualCPN.fire(transition);
        }
    }

    public void logStatus() {
        nodeIndividualCPNs.values().forEach(RuntimeIndividualCPN::logStatus);
    }
}
