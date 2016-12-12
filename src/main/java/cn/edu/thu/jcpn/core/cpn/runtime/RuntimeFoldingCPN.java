package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.places.Place.PlaceType;
import cn.edu.thu.jcpn.core.places.runtime.IOwner;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.Transition.TransitionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

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

        Map<PlaceType, List<Place>> places = graph.getPlaces().values().stream().collect(Collectors.groupingBy(p -> p.getType()));
        Map<TransitionType, List<Transition>> transitions = graph.getTransitions().values().stream().collect(Collectors.groupingBy(t -> t.getType()));

        final List<IOwner> targets = owners;
        owners.forEach(owner -> {
            RuntimeIndividualCPN individualCPN = new RuntimeIndividualCPN(owner);
            individualCPN.construct(places, transitions, targets);
            individualCPNs.put(owner, individualCPN);
        });

        compiled = true;
        return true;
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
