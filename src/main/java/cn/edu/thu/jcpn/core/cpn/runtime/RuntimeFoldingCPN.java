package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.ITarget;
import cn.edu.thu.jcpn.core.transitions.Transition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RuntimeFoldingCPN {

    private static Logger logger = LogManager.getLogger();

    private CPN graph;
    private List<IOwner> owners;
    private Map<IOwner, RuntimeIndividualCPN> individualCPNs;

    private boolean compiled = false;

    public RuntimeFoldingCPN(CPN graph, List<IOwner> owners) {
        this.graph = graph;
        this.owners = owners;
        individualCPNs = new HashMap<>();
        this.compile();
    }

    /**
     * unfold the CPN net.
     *
     * @return
     */
    public boolean compile() {
        if (compiled) return true;

        Collection<Place> places = graph.getPlaces().values();
        Collection<Transition> transitions = graph.getTransitions().values();

        Set<ITarget> targets = new HashSet<>(owners);
        for (IOwner owner : owners) {
            targets.remove(owner);
            RuntimeIndividualCPN individualCPN = new RuntimeIndividualCPN(owner, this);
            individualCPN.construct(targets, places, transitions);
            individualCPNs.put(owner, individualCPN);
            targets.add(owner);
        }

        compiled = true;
        return compiled;
    }

    public List<IOwner> getOwners() {
        return owners;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public RuntimeIndividualCPN getIndividualCPN(IOwner owner) {
        return individualCPNs.get(owner);
    }

    public void logStatus() {
        ;
    }
}
