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
    private Map<IOwner, RuntimeIndividualCPN> individualCPNs;

    private boolean compiled = false;

    public RuntimeFoldingCPN(CPN graph, List<IOwner> owners) {
        this.graph = graph;
        individualCPNs = new HashMap<>();
        compile(owners);
    }

    /**
     * unfold the CPN net.
     *
     * @return
     */
    public boolean compile(List<IOwner> owners) {
        if (compiled) return true;

        Collection<Place> places = graph.getPlaces().values();
        Collection<Transition> transitions = graph.getTransitions().values();

        Set<ITarget> targets = new HashSet<>(owners);
        for (IOwner owner : owners) {
            targets.remove(owner);
            RuntimeIndividualCPN individualCPN = new RuntimeIndividualCPN(owner);
            individualCPN.construct(targets, places, transitions);
            individualCPNs.put(owner, individualCPN);
            targets.add(owner);
        }

        places.forEach(place -> place.getInitialTokens().forEach((owner, targetTokens)
                -> individualCPNs.get(owner).getPlace(place.getId()).addTokens(targetTokens)));

        compiled = true;
        return compiled;
    }

    public Set<IOwner> getOwners() {
        return individualCPNs.keySet();
    }

    public boolean isCompiled() {
        return compiled;
    }

    public void logStatus() {
        ;
    }
}
