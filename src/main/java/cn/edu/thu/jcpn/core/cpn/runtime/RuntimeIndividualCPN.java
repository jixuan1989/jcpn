package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.PlaceManager.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.place.runtime.*;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.recoverer.Recoverer;
import cn.edu.thu.jcpn.core.transition.Transition;
import cn.edu.thu.jcpn.core.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.transition.condition.OutputToken;
import cn.edu.thu.jcpn.core.recoverer.runtime.RuntimeRecoverer;
import cn.edu.thu.jcpn.core.transition.runtime.RuntimeTransition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static cn.edu.thu.jcpn.core.cpn.runtime.RuntimeIndividualCPN.TokenType.*;
import static java.util.stream.Collectors.groupingBy;

public class RuntimeIndividualCPN {

    private static Logger logger = LogManager.getLogger();

    private static Random random = new Random();

    private INode owner;
    private Map<Integer, RuntimePlace> places;
    private Map<Integer, IPlaceMonitor> placeMonitors;

    private Map<Integer, RuntimeTransition> transitions;
    private Map<Integer, ITransitionMonitor> transitionMonitors;

    private Map<Integer, RuntimeRecoverer> recoverers;

    /**
     * enable transition order by priority.
     */
    private Map<Integer, List<RuntimeTransition>> priorityTransitions;

    private List<RuntimeRecoverer> canRunRecoverers;

    private RuntimeFoldingCPN foldingCPN;

    public RuntimeIndividualCPN(INode owner, RuntimeFoldingCPN foldingCPN) {
        this.owner = owner;
        places = new HashMap<>();
        transitions = new HashMap<>();

        placeMonitors = new HashMap<>();
        transitionMonitors = new HashMap<>();

        recoverers = new HashMap<>();

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

    public IPlaceMonitor getPlaceMonitor(int pid) {
        return placeMonitors.get(pid);
    }

    public ITransitionMonitor getTransitionMonitor(int tid) {
        return transitionMonitors.get(tid);
    }

    public void addMonitor(int pid, IPlaceMonitor monitor) {
        if (!places.containsKey(pid)) return;

        placeMonitors.put(pid, monitor);
    }

    private void addTransition(Transition transition) {
        transitions.put(transition.getId(), new RuntimeTransition(owner, transition, places, foldingCPN));
    }

    public void addMonitor(int tid, ITransitionMonitor monitor) {
        if (!transitions.containsKey(tid)) return;

        transitionMonitors.put(tid, monitor);
    }

    private void addRecoverer(Recoverer recoverer) {
        recoverers.put(recoverer.getId(), new RuntimeRecoverer(owner, recoverer, places));
    }

    /**
     * copy place and transition into this cpn instance.  Initial tokens are not included.
     * <br> Though global place have been included in place, we extract them into this instance again
     *
     * @param places      no matter whether place have global place, we do not copy the global place in them
     * @param transitions
     */
    public void construct(Collection<Place> places, Collection<Transition> transitions,
                          Collection<Recoverer> recoverers) {
        //copy all the place and transition first.
        places.forEach(this::addPlace);
        transitions.forEach(this::addTransition);
        recoverers.forEach(this::addRecoverer);
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
    public void addPlaceTokens(Integer pid, List<IToken> tokens) {
        RuntimePlace instance = places.get(pid);
        synchronized (instance) {
            instance.addTokens(tokens);
        }
    }

    /**
     * neaten places: for each place, reassign the tokens according their time.
     * For future tokens, assign them to the newly tokens if their time are arrived.
     * For tested tokens, assign them to the timeout tokens if their time are timeout.
     */
    public void neatenPlaces() {
        for (RuntimePlace place : places.values()) {
            int pid = place.getId();
            List<IToken> timeoutTokens = place.reassignTokens();
            transitions.values().forEach(transition -> transition.removeTokenFromCache(pid, timeoutTokens));
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
            //reportWhenTokenConsumed(places.get(pid), token, transition);
        });
        OutputToken outputToken = transition.firing(inputToken);
        //reportWhenTokensAdded(transition, outputToken);
        //reportWhenFiring(transition, inputToken, outputToken);
        return outputToken;
    }

    public boolean hasCanRunRecoverers() {
        this.canRunRecoverers = getCanRunRecoverers();
        return !this.canRunRecoverers.isEmpty();
    }

    private List<RuntimeRecoverer> getCanRunRecoverers() {
        return this.recoverers.values().stream().
                filter(RuntimeRecoverer::canRun).collect(Collectors.toList());
    }

    public void runRecoverers() {
        this.canRunRecoverers.forEach(RuntimeRecoverer::run);
    }

    private void reportWhenTokensAdded(RuntimeTransition transition, OutputToken outputToken) {
        for (Map.Entry<INode, Map<Integer, List<IToken>>> toPidTokens : outputToken.entrySet()) {
            INode to = toPidTokens.getKey();

            for (Map.Entry<Integer, List<IToken>> pidTokens : toPidTokens.getValue().entrySet()) {
                int pid = pidTokens.getKey();
                List<IToken> tokens = pidTokens.getValue();
                RuntimeIndividualCPN toCPN = foldingCPN.getIndividualCPN(to);
                IPlaceMonitor monitor = toCPN.getPlaceMonitor(pid);
                RuntimePlace toPlace = toCPN.getPlace(pid);
                monitor.reportAfterTokensAdded(toCPN.getOwner(), toPlace.getId(), toPlace.getName(),
                        tokens, owner, transition.getId(), transition.getName(), toPlace.getTimeoutTokens(),
                        toPlace.getTestedTokens(), toPlace.getNewlyTokens(), toPlace.getFutureTokens());

                Map<TokenType, Map<INode, Collection<IToken>>> pidAllTokens = getPidAllTokens(pid);
                monitor.reportAfterTokensAdded(owner, toPlace.getId(), toPlace.getName(), tokens, pidAllTokens.get(TIMEOUT),
                        pidAllTokens.get(TESTED), pidAllTokens.get(NEWLY), pidAllTokens.get(FUTURE));
            }
        }
    }

    private void reportWhenTokenConsumed(RuntimePlace place, IToken token, RuntimeTransition transition) {
        if (!placeMonitors.containsKey(place.getId())) return;

        IPlaceMonitor monitor = placeMonitors.get(place.getId());
        monitor.reportAfterTokenConsumed(owner, place.getId(), place.getName(), token, transition.getId(), transition.getName(),
                place.getTestedTokens(), place.getNewlyTokens(), place.getFutureTokens());

        Map<TokenType, Map<INode, Collection<IToken>>> pidAllTokens = getPidAllTokens(place.getId());
        monitor.reportAfterTokenConsumed(owner, place.getId(), place.getName(), token, transition.getId(), transition.getName(),
                pidAllTokens.get(TESTED), pidAllTokens.get(NEWLY), pidAllTokens.get(FUTURE));
    }

    private void reportWhenFiring(RuntimeTransition transition, InputToken inputToken, OutputToken outputToken) {
        if (!transitionMonitors.containsKey(transition.getId())) return;

        ITransitionMonitor monitor = transitionMonitors.get(transition.getId());
        monitor.reportWhenFiring(owner, transition.getId(), transition.getName(), inputToken, outputToken);
    }

    private Map<TokenType, Map<INode, Collection<IToken>>> getPidAllTokens(int pid) {
        Map<TokenType, Map<INode, Collection<IToken>>> res = new HashMap<>();
        Map<INode, Collection<IToken>> timeout = new HashMap<>();
        Map<INode, Collection<IToken>> tested = new HashMap<>();
        Map<INode, Collection<IToken>> newly = new HashMap<>();
        Map<INode, Collection<IToken>> future = new HashMap<>();
        foldingCPN.getOwners().forEach(owner -> {
            RuntimePlace runtimePlace = foldingCPN.getIndividualCPN(owner).getPlace(pid);
            timeout.put(owner, runtimePlace.getTimeoutTokens());
            tested.put(owner, runtimePlace.getTestedTokens());
            newly.put(owner, runtimePlace.getNewlyTokens());
            future.put(owner, runtimePlace.getFutureTokens());
        });
        res.put(TIMEOUT, tested);
        res.put(TESTED, tested);
        res.put(NEWLY, newly);
        res.put(FUTURE, future);
        return res;
    }

    enum TokenType {
        TIMEOUT, TESTED, NEWLY, FUTURE
    }

    @Override
    public String toString() {
        return "ICPN [owner=" + owner + ", place=" + places + ", transition=" + transitions + "]";
    }

    public void logStatus() {
        System.out.println("--------------------------------node: " + owner + "----------------------------------");
        places.values().forEach(RuntimePlace::logStatus);
    }
}
