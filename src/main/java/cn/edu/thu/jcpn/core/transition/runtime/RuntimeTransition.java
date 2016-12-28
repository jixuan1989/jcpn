package cn.edu.thu.jcpn.core.transition.runtime;

import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeIndividualCPN;
import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.place.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.transition.Transition;
import cn.edu.thu.jcpn.core.transition.Transition.TransitionType;
import cn.edu.thu.jcpn.core.transition.condition.Condition;
import cn.edu.thu.jcpn.core.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.transition.condition.OutputToken;
import cn.edu.thu.jcpn.core.transition.condition.PlacePartition;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transition is the minimum unit to execute an event. Firstly, the individualCPN calls the
 * hasEnableTransitions method of its transition to get the transition who can execute. Then the CPN random
 * pick one transition to prepare to execute through call the specific transition's getRandmonInputToken
 * and get the inputToken from the transition' cache. Then, use the inputToken the make the transition
 * firing. After that, use the inputToken to clean the relative tokens in all relative transition' caches,
 * and the original place.
 */
public class RuntimeTransition {

    private INode owner;
    private int id;
    private String name;
    private int priority = 500;
    private TransitionType type;

    private Condition condition;
    private Function<InputToken, OutputToken> outputFunction;

    private Map<Integer, Integer> inPidPriorities;
    private Map<Integer, RuntimePlace> inPlaces;

    // <to, <pid, place>>
    private Map<INode, Map<Integer, RuntimePlace>> outPlaces;

    private Map<PlacePartition, List<InputToken>> cache;

    /**
     * Only use for fetch the place of tokens' targets.
     */
    private RuntimeFoldingCPN foldingCPN;

    private GlobalClock globalClock;

    public RuntimeTransition(INode owner, Map<Integer, RuntimePlace> runtimePlaces,
                             Transition transition, RuntimeFoldingCPN foldingCPN) {
        this.owner = owner;
        this.id = transition.getId();
        this.name = transition.getName();
        this.priority = transition.getPriority();
        this.type = transition.getType();

        this.condition = transition.getCondition();
        this.outputFunction = transition.getOutputFunction();

        this.inPidPriorities = transition.getInPidPriorities();
        this.inPlaces = new HashMap<>();
        this.outPlaces = new HashMap<>();
        initInPlaces(transition.getInPlaces(), runtimePlaces);
        initCache();

        this.foldingCPN = foldingCPN;

        this.globalClock = GlobalClock.getInstance();
    }

    private void initInPlaces(Map<Integer, Place> places, Map<Integer, RuntimePlace> runtimePlaces) {
        places.keySet().forEach(pid -> this.inPlaces.put(pid, runtimePlaces.get(pid)));
    }

    private void initCache() {
        cache = new HashMap<>();
        Collection<PlacePartition> partitions = condition.getPlacePartition();
        partitions.forEach(partition -> partition.setPriorities(inPidPriorities));
        partitions.forEach(partition -> cache.put(partition, new ArrayList<>()));
        PlacePartition freePartition = getFreePartition();
        freePartition.forEach(pid -> {
            PlacePartition partition = new PlacePartition();
            partition.add(pid);
            partition.setPriorities(inPidPriorities);
            cache.put(partition, new ArrayList<>());
        });
    }

    private PlacePartition getFreePartition() {
        PlacePartition completePartition = PlacePartition.generate(inPlaces.values());
        PlacePartition conditionPartition = PlacePartition.combine(condition.getPlacePartition());
        return completePartition.subtract(conditionPartition);
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public INode getOwner() {
        return owner;
    }

    public int getPriority() {
        return priority;
    }

    public Condition getCondition() {
        return condition;
    }

    public Map<Integer, RuntimePlace> getInPlaces() {
        return inPlaces;
    }

    public Map<INode, Map<Integer, RuntimePlace>> getOutPlaces() {
        return outPlaces;
    }

    public Function<InputToken, OutputToken> getOutputFunction() {
        return outputFunction;
    }

    /**
     * when new tokens enter its input place, which related to the transition, this method needs to be called.
     * <br> the method will update its(transition's) cachedBinding
     * <br> NOTICE: this class is not idempotent. That is to say, it assumes after checkNewlyTokens4Firing,
     * <br> all the newly tokens are moved into tested.
     * <br> (However, this method does not implement it, a CPNInstance class needs to control that)
     */
    public void checkNewlyTokens4Firing() {
        // for each partition, ask input place for tokens available for the to.
        cache.keySet().forEach(this::checkNewlyTokens4Firing);
    }

    public void checkNewlyTokens4Firing(PlacePartition partition) {
        List<InputToken> availableTokens = new ArrayList<>();
        InputToken tokenSet = new InputToken();
        findAndSave(partition, tokenSet, availableTokens, 0);
        // Map<INode, Map<PlacePartition, List<InputToken>>> cache;
        cache.get(partition).addAll(availableTokens);
    }

    private void findAndSave(PlacePartition partition, InputToken tokenSet, List<InputToken> availableTokens, int position) {
        List<Integer> pids = partition.getPids();
        if (position == pids.size()) {
            if (condition.test(partition, tokenSet) && containNew(partition, tokenSet)) {
                availableTokens.add(new InputToken(tokenSet));
            }
            return;
        }

        RuntimePlace place = inPlaces.get(pids.get(position));
        List<IToken> tokens = new ArrayList<>();
        if (place.hasNewlyTokens()) tokens.addAll(place.getNewlyTokens());
        tokens.addAll(place.getTestedTokens());

        for (int i = 0; i < tokens.size(); ++i) {
            tokenSet.addToken(pids.get(position), tokens.get(i));
            findAndSave(partition, tokenSet, availableTokens, position + 1);
            tokenSet.removeToken(pids.get(position));
        }
    }

    private boolean containNew(PlacePartition partition, InputToken inputToken) {
        for (int pid : partition) {
            List<IToken> tokens = inPlaces.get(pid).getNewlyTokens();
            IToken token = inputToken.get(pid);
            if (tokens.contains(token)) {
                return true;
            }
        }

        return false;
    }

    public boolean canFire() {
        return cache.values().stream().noneMatch(List::isEmpty);
    }

    public InputToken getRandmonInputToken() {
        InputToken inputToken = new InputToken();
        cache.values().forEach(partitionTokens -> inputToken.merge(partitionTokens.get(0)));
        return inputToken;
    }

    /**
     * @param inputTokens
     * @return
     */
    public OutputToken firing(InputToken inputTokens) {
        if (outputFunction == null) return null;
        OutputToken outputToken = this.outputFunction.apply(inputTokens);

        for (Entry<INode, Map<Integer, List<IToken>>> toPidTokens : outputToken.entrySet()) {
            INode to = toPidTokens.getKey();

            for (Entry<Integer, List<IToken>> pidTokens : toPidTokens.getValue().entrySet()) {
                int pid = pidTokens.getKey();
                List<IToken> tokens = pidTokens.getValue();
                tokens.forEach(token -> token.setOwner(to));
                RuntimePlace toPlace = getOutPlace(to, pid);
                toPlace.addTokens(tokens);

                tokens.forEach(token -> {
                    // register a event in the timeline.
                    long time = token.getTime();
                    if (owner.equals(to)) {
                        globalClock.addAbsoluteTimepointForRunning(owner, time);
                    } else {
                        globalClock.addAbsoluteTimepointForSending(to, time);
                    }
                });
            }
        }
        return outputToken;
    }

    private RuntimePlace getOutPlace(INode to, int pid) {
        Map<Integer, RuntimePlace> toPlaces = outPlaces.computeIfAbsent(to, obj -> new HashMap<>());
        if (toPlaces.isEmpty() || !toPlaces.containsKey(pid)) {
            RuntimeIndividualCPN toCPN = foldingCPN.getIndividualCPN(to);
            RuntimePlace toPlace = toCPN.getPlace(pid);
            toPlaces.put(pid, toPlace);
        }
        return toPlaces.get(pid);
    }

    public Map<PlacePartition, List<InputToken>> getAllAvailableTokens() {
        return cache;
    }

    /**
     * remove all the candidate bindings related with the given token.
     * <br> this method needs to be called if you find that you can not get the token any more from the input place, while the token is still in cached bindings.
     * <br> that is to say, you should get a token firstly and then remove the token from cache immediately using this function.
     * <br> (only multi-threads mode requires)
     *
     * @param inputTokens
     */
    public void removeTokenFromCache(InputToken inputTokens) {
        for (Entry<Integer, IToken> inputToken : inputTokens.entrySet()) {
            int pid = inputToken.getKey();
            IToken token = inputToken.getValue();

            for (Entry<PlacePartition, List<InputToken>> partitionEntry : cache.entrySet()) {
                PlacePartition partition = partitionEntry.getKey();
                if (!partition.contains(pid)) continue;

                List<InputToken> tokenSets = partitionEntry.getValue();
                List<InputToken> removedTokenSets = tokenSets.stream().
                        filter(tokenSet -> tokenSet.containsValue(token)).collect(Collectors.toList());
                tokenSets.removeAll(removedTokenSets);
            }
        }
    }
}