package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.places.runtime.*;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.ITarget;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.condition.InputToken;
import cn.edu.thu.jcpn.core.transitions.condition.OutputToken;
import cn.edu.thu.jcpn.core.transitions.runtime.RuntimeTransition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class RuntimeIndividualCPN {

    private static Logger logger = LogManager.getLogger();

    private static Random random = new Random();

    private GlobalClock globalClock = GlobalClock.getInstance();

    private IOwner owner;
    private Map<Integer, RuntimePlace> places;
    private Map<Integer, RuntimeTransition> transitions;

    private RuntimeFoldingCPN foldingCPN;

    public RuntimeIndividualCPN(IOwner owner, RuntimeFoldingCPN foldingCPN) {
        this.owner = owner;
        places = new HashMap<>();
        transitions = new HashMap<>();

        this.foldingCPN = foldingCPN;
    }

    public IOwner getOwner() {
        return owner;
    }

    public void setOwner(IOwner owner) {
        this.owner = owner;
    }

    public Map<Integer, RuntimePlace> getPlaces() {
        return places;
    }

    public void setPlaces(Map<Integer, RuntimePlace> places) {
        this.places = places;
    }

    public Map<Integer, RuntimeTransition> getTransitions() {
        return transitions;
    }

    public void setTransitions(Map<Integer, RuntimeTransition> transitions) {
        this.transitions = transitions;
    }

    public RuntimePlace getPlace(Integer id) {
        return this.places.get(id);
    }

    private void addRuntimePlace(Place place) {
        places.put(place.getId(), new RuntimePlace(owner, place));
    }

    private void addRuntimeTransition(Set<ITarget> targets, Transition transition) {
        transitions.put(transition.getId(), new RuntimeTransition(owner, targets, transition, places, foldingCPN));
    }

    /**
     * copy places and transitions into this cpn instance.  Initial tokens are not included.
     * <br> Though global places have been included in places, we extract them into this instance again
     *
     * @param places      no matter whether places have global places, we do not copy the global places in them
     * @param transitions
     */
    public void construct(Set<ITarget> targets, Collection<Place> places, Collection<Transition> transitions) {
        //copy all the places and transitions first.
        places.forEach(this::addRuntimePlace);
        transitions.forEach(transition -> addRuntimeTransition(targets, transition));
    }

    /**
     * this is thread safe.
     * <br>
     * only used for individual and global places
     * <br> this method do not register any events on the timeline.
     *
     * @param pid
     * @param tokens
     */
    public void addLocalNewlyTokens(Integer pid, List<IToken> tokens) {
        RuntimePlace instance = places.get(pid);
        synchronized (instance) {
            instance.addTokens(tokens);
        }
    }

    /**
     * process all the newly tokens and update transitions' cached, then mark these tokens as tested.
     * this method is idempotent
     */
    public void notifyTransitions() {
        transitions.values().forEach(RuntimeTransition::checkNewlyTokens4Firing);
        places.values().forEach(RuntimePlace::markTokensAsTested);
    }

    /**
     * check which transitions can be fired
     *
     * @return
     */
    public boolean hasEnableTransitions() {
        List<RuntimeTransition> enableTransitions = this.getEnableTransitions();
        return enableTransitions.size() > 0;
    }

    private List<RuntimeTransition> getEnableTransitions() {
        return this.transitions.values().stream().
                filter(RuntimeTransition::canFire).collect(Collectors.toList());
    }

    public RuntimeTransition randomEnable() {
        List<RuntimeTransition> enableTransitions = this.getEnableTransitions();
        return enableTransitions.get(random.nextInt(enableTransitions.size()));
    }

    public OutputToken firing(RuntimeTransition transition) {
        InputToken inputToken = transition.getRandmonInputToken();
        transitions.values().forEach(innerTransition -> innerTransition.removeTokenFromCache(inputToken));
        inputToken.forEach((pid, token) -> places.get(pid).removeTokenFromTest(token));
        return transition.firing(inputToken);
    }

    @Override
    public String toString() {
        return "ICPN [owner=" + owner + ", places=" + places + ", transitions=" + transitions + "]";
    }

    public void logStatus() {
        places.values().forEach(RuntimePlace::logStatus);
    }
}
