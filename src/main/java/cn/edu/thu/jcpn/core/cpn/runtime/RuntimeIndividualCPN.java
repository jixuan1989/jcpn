package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.place.runtime.*;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
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

    private INode owner;
    private Map<Integer, RuntimePlace> places;
    private Map<Integer, IPlaceMonitor> placeMonitors;

    private Map<Integer, RuntimeTransition> transitions;
    private Map<Integer, ITransitionMonitor> transitionMonitors;

    /**
     * enable transition order by priority.
     */
    private Map<Integer, List<RuntimeTransition>> priorityTransitions;

    private RuntimeFoldingCPN foldingCPN;

    public RuntimeIndividualCPN(INode owner, RuntimeFoldingCPN foldingCPN) {
        this.owner = owner;
        places = new HashMap<>();
        transitions = new HashMap<>();

        placeMonitors = new HashMap<>();
        transitionMonitors = new HashMap<>();

        this.foldingCPN = foldingCPN;
    }

    public INode getOwner() {
        return owner;
    }

    public void setOwner(INode owner) {
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

    private void addPlace(Place place) {
        places.put(place.getId(), new RuntimePlace(owner, place));
    }

    public void addMonitor(int pid, IPlaceMonitor monitor) {
        if (!places.containsKey(pid)) return;

        placeMonitors.put(pid, monitor);
    }

    private void addTransition(Transition transition) {
        transitions.put(transition.getId(), new RuntimeTransition(owner, places, transition, foldingCPN));
    }

    public void addMonitor(int tid, ITransitionMonitor monitor) {
        if (!transitions.containsKey(tid)) return;

        transitionMonitors.put(tid, monitor);
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
        places.forEach(this::addPlace);
        transitions.forEach(this::addTransition);
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
        Integer[] proritys = priorityTransitions.keySet().toArray(new Integer[]{});
        if (proritys.length == 0) return null;
        int randonPrority = proritys[random.nextInt(proritys.length)];

        List<RuntimeTransition> transitions = priorityTransitions.get(randonPrority);
        if (transitions.isEmpty()) return null;
        return transitions.get(random.nextInt(transitions.size()));
    }

    public RuntimeTransition enableByPriority() {
        Integer[] proritys = priorityTransitions.keySet().toArray(new Integer[]{});
        if (proritys.length == 0) return null;
        int highestPrority = proritys[0];

        List<RuntimeTransition> transitions = priorityTransitions.get(highestPrority);
        if (transitions.isEmpty()) return null;
        return transitions.get(random.nextInt(transitions.size()));
    }

    public OutputToken firing(RuntimeTransition transition) {
        InputToken inputToken = transition.getRandmonInputToken();
        transitions.values().forEach(innerTransition -> innerTransition.removeTokenFromCache(inputToken));
        inputToken.forEach((pid, token) -> {
            places.get(pid).removeTokenFromTest(token);
            reportWhenConsume(places.get(pid), token, transition);
        });
        OutputToken outputToken = transition.firing(inputToken);

        reportWhenFiring(transition, inputToken, outputToken);
        return outputToken;
    }

    private void reportWhenConsume(RuntimePlace place, IToken token, RuntimeTransition transition) {
        if (!placeMonitors.containsKey(place.getId())) return;

        IPlaceMonitor monitor = placeMonitors.get(place.getId());
        monitor.reportWhenConsume(owner, place.getId(), place.getName(), token, transition.getId(), transition.getName(),
                place.getTestedTokens(), place.getNewlyTokens(), place.getFutureTokens());
    }

    private void reportWhenFiring(RuntimeTransition transition, InputToken inputToken, OutputToken outputToken) {
        if (!transitionMonitors.containsKey(transition.getId())) return;

        ITransitionMonitor monitor = transitionMonitors.get(transition.getId());
        monitor.reportWhenFiring(owner, transition.getId(), transition.getName(), inputToken, outputToken);
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
