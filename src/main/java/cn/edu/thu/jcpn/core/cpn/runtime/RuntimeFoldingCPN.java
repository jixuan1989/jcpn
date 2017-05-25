package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.common.Triple;
import cn.edu.thu.jcpn.core.container.IContainer;
import cn.edu.thu.jcpn.core.container.runtime.InsertAgencyManager;
import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.monitor.IStorageMonitor;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.executor.recoverer.Recoverer;
import cn.edu.thu.jcpn.core.executor.transition.Transition;
import cn.edu.thu.jcpn.core.executor.transition.runtime.RuntimeTransition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType.LOCAL;

public class RuntimeFoldingCPN {

    private AtomicInteger totalTimes = new AtomicInteger(0);

    private static Logger logger = LogManager.getLogger();

    private String version;

    // an Engine can be auto(true) mode or manual mode(false).
    private boolean mode;

    // a CPN represents one part of the system. Servers in a cluster is a cpn, also are the clients.
    private Set<CPN> cpns;

    // all nodes in the foldingCPN, no matter servers or clients.
    private List<INode> nodes;

    // all individual part of the foldingCPN, each one represents a real node, likes a server or a client.
    private Map<INode, RuntimeIndividualCPN> nodeIndividualCPNs;

    private InsertAgencyManager insertAgencyManager;

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

        InsertAgencyManager.init(this);
        insertAgencyManager = InsertAgencyManager.getInstance();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean getMode() {
        return mode;
    }

    public void setMode(boolean mode) {
        this.mode = mode;
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

    public void addMonitor(int sid, IStorageMonitor monitor) {
        nodeIndividualCPNs.values().stream().filter(individualCPN -> individualCPN.getContainers().containsKey(sid)).
                forEach(individualCPN -> addMonitor(individualCPN.getOwner(), sid, monitor));
    }

    public void addMonitor(INode node, int sid, IStorageMonitor monitor) {
        if (!nodeIndividualCPNs.containsKey(node) || !nodeIndividualCPNs.get(node).getContainers().containsKey(sid))
            return;

        nodeIndividualCPNs.get(node).addMonitor(sid, monitor);
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
            RuntimeIndividualCPN individualCPN = new RuntimeIndividualCPN(node, this, mode);
            individualCPN.construct(containers, transitions, recoverers);
            nodeIndividualCPNs.put(node, individualCPN);

            globalClock.addAbsoluteTimePointForLocalHandle(node, 0L);
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
     * @return whether the remote queue has events to do.
     */
    public boolean hasNextRemoteTime() {
        return !isTimeout() && globalClock.hasNextRemoteTime();
    }

    /**
     * @return whether the local queue has events to do.
     */
    public boolean hasNextLocalTime() {
        return !isTimeout() && globalClock.hasNextLocalTime();
    }

    public boolean nextRound(long start, int times) {
        if (!isTimeout() && !globalClock.hasNextTime()) {
            return false;
        }

        Triple<EventType, Long, Set<INode>> nextEventTimeNodes = globalClock.timeElapse();
        if (null == nextEventTimeNodes) {
            return false;
        }

        EventType eventType = nextEventTimeNodes.getLeft();
        Set<INode> eventNodes = nextEventTimeNodes.getRight();

        logger.trace(() -> String.format("Run %s events... %s", eventType.toString(), eventNodes));
        if (eventType.equals(LOCAL)) {
            runLocalEvents(eventNodes, start, times);
        } else {
            runRemoteEvents(eventNodes);
        }

        return true;
    }

    private void runRemoteEvents(Set<INode> eventNodes) {
        eventNodes.parallelStream().forEach(eventNode -> insertAgencyManager.runAgencyEvents(eventNode));
        //nodes.forEach(node -> insertAgencyManager.runAgencyEvents(node));
    }

    private void runLocalEvents(Set<INode> eventNodes, long start, int times) {
        eventNodes.parallelStream().forEach(eventNode -> this.runNodeLocalEvents(start, times, eventNode));
        //nodes.forEach(node -> this.runNodeLocalEvents(start, times, node));
    }

    private void runNodeLocalEvents(long start, int times, INode node) {
        RuntimeIndividualCPN individualCPN = getIndividualCPN(node);

        individualCPN.neatenContainers();
        if (individualCPN.hasEnableRecoverers()) {
            individualCPN.fireAllRecoverers();
            individualCPN.neatenContainers();
        }

        individualCPN.notifyTransitions();

        int count = 0;
        while (individualCPN.hasEnableTransitions()) {
            RuntimeTransition transition = individualCPN.randomEnable();
            individualCPN.fire(transition);
            ++count;
        }

        int totalCount = totalTimes.addAndGet(count);
        if (totalCount >= times) {
            long end = System.currentTimeMillis();
            System.out.println((end - start) + "," + totalCount);
            System.exit(0);
        }

    }
}
