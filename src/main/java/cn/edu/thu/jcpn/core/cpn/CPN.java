package cn.edu.thu.jcpn.core.cpn;

import cn.edu.thu.jcpn.core.container.IContainer;
import cn.edu.thu.jcpn.core.executor.recoverer.Recoverer;
import cn.edu.thu.jcpn.core.executor.transition.Transition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CPN {

    private static int count = 0; // id counter of the CPN.

    private int id;
    private String name;

    private Map<Integer, IContainer> containers;
    private Map<Integer, Transition> transitions;
    private Map<Integer, Recoverer> recoverers;

    public CPN(String name) {
        this.id = count++;
        this.name = name;

        this.containers = new HashMap<>();
        this.transitions = new HashMap<>();
        this.recoverers = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<Integer, IContainer> getContainers() {
        return containers;
    }

    public void addContainers(IContainer... containers) {
        Arrays.stream(containers).forEach(this::addContainer);
    }

    public void addContainer(IContainer container) {
        containers.put(container.getId(), container);
    }

    public Map<Integer, Transition> getTransitions() {
        return transitions;
    }

    public void addTransitions(Transition... transitions) {
        Arrays.stream(transitions).forEach(this::addTransition);
    }

    public void addTransition(Transition transition) {
        transitions.put(transition.getId(), transition);
    }

    public Map<Integer, Recoverer> getRecoverers() {
        return recoverers;
    }

    public void addRecoverers(Recoverer... recoverers) {
        Arrays.stream(recoverers).forEach(this::addRecoverer);
    }

    public void addRecoverer(Recoverer recoverer) {
        recoverers.put(recoverer.getId(), recoverer);
    }
}
