package cn.edu.thu.jcpn.core.transitions.runtime;

import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeIndividualCPN;
import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.places.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.ITarget;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.runtime.tokens.LocalAsTarget;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.condition.Condition;
import cn.edu.thu.jcpn.core.transitions.condition.InputToken;
import cn.edu.thu.jcpn.core.transitions.condition.OutputToken;
import cn.edu.thu.jcpn.core.transitions.condition.PlacePartition;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transition is the minimum unit to execute an event. Firstly, the individualCPN calls the
 * hasEnableTransitions method of its transitions to get the transitions who can execute. Then the CPN random
 * pick one transition to prepare to execute through call the specific transition's getRandmonInputToken
 * and get the inputToken from the transition' cache. Then, use the inputToken the make the transition
 * firing. After that, use the inputToken to clean the relative tokens in all relative transitions' caches,
 * and the original places.
 */
public class RuntimeTransition {

    private int id;
    private String name;
    private IOwner owner;
    private int priority = 500;

    private Map<Integer, RuntimePlace> inPlaces;
    private Map<ITarget, Map<Integer, RuntimePlace>> outPlaces;

    private Condition condition;
    private Function<InputToken, OutputToken> outputFunction;

    private Map<PlacePartition, List<InputToken>> cache;

    private GlobalClock globalClock;

    /**
     * Only use for fetch the places of tokens' targets.
     */
    private RuntimeFoldingCPN foldingCPN;

    public RuntimeTransition(IOwner owner, Map<Integer, RuntimePlace> runtimePlaces,
                             Transition transition, RuntimeFoldingCPN foldingCPN) {

        this.id = transition.getId();
        this.name = transition.getName();
        this.owner = owner;

        initInPlaces(transition.getInPlaces(), runtimePlaces);
        outPlaces = new HashMap<>();

        condition = transition.getCondition();
        outputFunction = transition.getOutputFunction();

        initCache();

        globalClock = GlobalClock.getInstance();

        this.foldingCPN = foldingCPN;
    }

    private void initInPlaces(Set<Place> places, Map<Integer, RuntimePlace> runtimePlaces) {
        inPlaces = new HashMap<>();
        places.forEach(place -> {
            RuntimePlace inPlace = runtimePlaces.get(place.getId());
            inPlaces.put(inPlace.getId(), inPlace);
        });
    }

    private void initCache() {
        cache = new HashMap<>();
        condition.getPlacePartition().forEach(partition -> cache.put(partition, new ArrayList<>()));
        PlacePartition freePartition = getFreePartition();
        freePartition.forEach(pid -> {
            PlacePartition partition = new PlacePartition();
            partition.add(pid);
            cache.put(partition, new ArrayList<>());
        });
    }

    private PlacePartition getFreePartition() {
        PlacePartition conditionPartition = PlacePartition.combine(condition.getPlacePartition());
        PlacePartition completePartition = PlacePartition.generate(inPlaces.values());
        PlacePartition complementPartition = completePartition.subtract(conditionPartition);

        return complementPartition;
    }

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IOwner getOwner() {
        return owner;
    }

    public void setOwner(IOwner owner) {
        this.owner = owner;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Map<Integer, RuntimePlace> getInPlaces() {
        return inPlaces;
    }

    public void setInPlaces(Map<Integer, RuntimePlace> inPlaces) {
        this.inPlaces = inPlaces;
    }

    public Map<ITarget, Map<Integer, RuntimePlace>> getOutPlaces() {
        return outPlaces;
    }

    public void setOutPlaces(Map<ITarget, Map<Integer, RuntimePlace>> outPlaces) {
        this.outPlaces = outPlaces;
    }

    public Function<InputToken, OutputToken> getOutputFunction() {
        return outputFunction;
    }

    public void setOutputFunction(Function<InputToken, OutputToken> outputFunction) {
        this.outputFunction = outputFunction;
    }

    /**
     * when new tokens enter its input places, which related to the transitions, this method needs to be called.
     * <br> the method will update its(transitions's) cachedBinding
     * <br> NOTICE: this class is not idempotent. That is to say, it assumes after checkNewlyTokens4Firing,
     * <br> all the newly tokens are moved into tested.
     * <br> (However, this method does not implement it, a CPNInstance class needs to control that)
     */
    public void checkNewlyTokens4Firing() {
        // for each partition, ask input places for tokens available for the target.
        cache.keySet().forEach(this::checkNewlyTokens4Firing);
    }

    public void checkNewlyTokens4Firing(PlacePartition partition) {
        List<InputToken> availableTokens = new ArrayList<>();
        InputToken tokenSet = new InputToken();
        findAndSave(partition, tokenSet, availableTokens, 0);
        // Map<ITarget, Map<PlacePartition, List<InputToken>>> cache;
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
        OutputToken outputTokens = this.outputFunction.apply(inputTokens);

        for (Entry<ITarget, Map<Integer, List<IToken>>> targetPidTokens : outputTokens.entrySet()) {
            ITarget target = targetPidTokens.getKey();

            long time = globalClock.getTime();
            for (Entry<Integer, List<IToken>> pidTokens : targetPidTokens.getValue().entrySet()) {
                int pid = pidTokens.getKey();
                List<IToken> tokens = pidTokens.getValue();
                RuntimePlace targetPlace = getOutPlace(target, pid);
                targetPlace.addTokens(tokens);

                time = tokens.get(0).getTime();
            }
            // register a event in the timeline.
            if (target instanceof LocalAsTarget) {
                globalClock.addAbsoluteTimepointForRunning(owner, time);
            } else {
                globalClock.addAbsoluteTimepointForSending((IOwner) target, time);
            }
        }
        return outputTokens;
    }

    private RuntimePlace getOutPlace(ITarget target, int pid) {
        Map<Integer, RuntimePlace> targetPlaces = outPlaces.computeIfAbsent(target, obj -> new HashMap<>());
        if (targetPlaces.isEmpty() || !targetPlaces.containsKey(pid)) {
            RuntimeIndividualCPN targetCPN = foldingCPN.getIndividualCPN((IOwner) target, owner);
            RuntimePlace targetPlace = targetCPN.getPlace(pid);
            targetPlaces.put(pid, targetPlace);
        }
        return targetPlaces.get(pid);
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