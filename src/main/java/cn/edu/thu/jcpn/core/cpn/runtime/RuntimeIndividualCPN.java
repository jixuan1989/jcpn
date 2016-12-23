package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.place.runtime.*;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.transition.Transition;
import cn.edu.thu.jcpn.core.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.transition.condition.OutputToken;
import cn.edu.thu.jcpn.core.transition.runtime.RuntimeTransition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;

public class RuntimeIndividualCPN {

    private static Logger logger = LogManager.getLogger();

    private static Random random = new Random();

    private IOwner owner;
    private Map<Integer, RuntimePlace> places;
    private Map<Integer, RuntimeTransition> transitions;

    /**
     * enable transition order by priority.
     */
    private Map<Integer, List<RuntimeTransition>> priorityTransitions;

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

    private void addRuntimeTransition(Transition transition) {
        transitions.put(transition.getId(), new RuntimeTransition(owner, places, transition, foldingCPN));
    }

    /**
     * copy place and transition into this cpn instance.  Initial tokens are not included.
     * <br> Though global place have been included in place, we extract them into this instance again
     *
     * @param places      no matter whether place have global place, we do not copy the global place in them
     * @param transitions
     */
    public void construct(Collection<Place> places, Collection<Transition> transitions) {
        //copy all the place and transition first.
        places.forEach(this::addRuntimePlace);
        transitions.forEach(this::addRuntimeTransition);
    }

    /**
     * this is thread safe.
     * <br>
     * only used for individual and global place
     * <br> this method do not register any events on the timeline.
     *
     * @param pid
     * @param tokens
     */
    public void addNewlyTokens(Integer pid, List<IToken> tokens) {
        RuntimePlace instance = places.get(pid);
        synchronized (instance) {
            instance.addTokens(tokens);
        }
    }

    /**
     * process all the newly tokens and update transition' cached, then mark these tokens as tested.
     * this method is idempotent
     */
    public void notifyTransitions() {
        transitions.values().forEach(RuntimeTransition::checkNewlyTokens4Firing);
        places.values().forEach(RuntimePlace::markTokensAsTested);
    }

    /**
     * check which transition can be fired
     *
     * @return
     */
    public boolean hasEnableTransitions() {
        priorityTransitions = this.getEnableTransitions();
        return priorityTransitions.size() > 0;
    }

    //TODO order by desc or asc?
    private Map<Integer, List<RuntimeTransition>> getEnableTransitions() {
        return this.transitions.values().stream().
                filter(RuntimeTransition::canFire).collect(groupingBy(RuntimeTransition::getPriority));
    }

    public RuntimeTransition randomEnable() {
        Integer[] proritys = priorityTransitions.keySet().toArray(new Integer[] {});
        if (proritys.length == 0) return null;
        int randonPrority = proritys[random.nextInt(proritys.length)];

        List<RuntimeTransition> transitions = priorityTransitions.get(randonPrority);
        if (transitions.isEmpty()) return null;
        return transitions.get(random.nextInt(transitions.size()));
    }

    public RuntimeTransition enableByPriority() {
        Integer[] proritys = priorityTransitions.keySet().toArray(new Integer[] {});
        if (proritys.length == 0) return null;
        int highestPrority = proritys[0];

        List<RuntimeTransition> transitions = priorityTransitions.get(highestPrority);
        if (transitions.isEmpty()) return null;
        return transitions.get(random.nextInt(transitions.size()));
    }

    public OutputToken firing(RuntimeTransition transition) {
        InputToken inputToken = transition.getRandmonInputToken();
        transitions.values().forEach(innerTransition -> innerTransition.removeTokenFromCache(inputToken));
        inputToken.forEach((pid, token) -> places.get(pid).removeTokenFromTest(token));
        return transition.firing(inputToken);
    }

    @Override
    public String toString() {
        return "ICPN [owner=" + owner + ", place=" + places + ", transition=" + transitions + "]";
    }

    public void logStatus() {
        System.out.println("--------------------------------owner: " + owner + "----------------------------------");
        places.values().forEach(RuntimePlace::logStatus);
    }
}
