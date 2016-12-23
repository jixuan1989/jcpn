package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.LocalAsTarget;
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

        for (IOwner owner : owners) {
            RuntimeIndividualCPN individualCPN = new RuntimeIndividualCPN(owner, this);
            individualCPN.construct(places, transitions);
            individualCPNs.put(owner, individualCPN);
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

    public RuntimeIndividualCPN getIndividualCPN(IOwner owner, IOwner from) {
        IOwner realOwner = (owner instanceof LocalAsTarget) ? from : owner;
        return individualCPNs.get(realOwner);
    }

    public void logStatus() {
        individualCPNs.values().forEach(RuntimeIndividualCPN::logStatus);
    }
}
