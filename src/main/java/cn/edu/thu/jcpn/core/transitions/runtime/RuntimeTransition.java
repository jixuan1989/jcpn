package cn.edu.thu.jcpn.core.transitions.runtime;

import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeIndividualCPN;
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
    private Set<ITarget> targets;

    private Map<Integer, RuntimePlace> inPlaces;
    private Map<ITarget, Map<Integer, RuntimePlace>> outPlaces;

    private Condition condition;
    private Function<InputToken, OutputToken> outputFunction;

    private Map<ITarget, Map<PlacePartition, List<InputToken>>> cache;
    private List<ITarget> enableTargets;

    private GlobalClock globalClock;

    private static Random random = new Random();

    /**
     * Only use for fetch the places of tokens' targets.
     */
    private RuntimeFoldingCPN foldingCPN;

    public RuntimeTransition(IOwner owner, Set<ITarget> targets, Transition transition, RuntimeFoldingCPN foldingCPN) {
        this.owner = owner;
        this.targets = new HashSet<>(targets);

        id = transition.getId();
        name = transition.getName();
        condition = transition.getCondition();
        outputFunction = transition.getOutputFunction();

        initCache();
        enableTargets = new ArrayList<>();

        this.foldingCPN = foldingCPN;

        globalClock = GlobalClock.getInstance();
    }

    public void initCache() {
        cache = new HashMap<>();
        targets.forEach(target -> {
            Map<PlacePartition, List<InputToken>> targetPartitions = cache.computeIfAbsent(target, obj -> new HashMap<>());
            condition.getPlacePartition().forEach(partition -> targetPartitions.put(partition, new ArrayList<>()));
        });
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
        // for each target, ask input places for tokens available for the target.
        targets.forEach(this::checkNewlyTokens4Firing);
    }

    /**
     * For each input place, get the newly tokens which will be transmitted to the target place remotely.
     * If the target place is locally, then getNewlyTokens action will get the newly tokens locally.
     * <pre>
     * It means there are two situations:
     *      1) the transition is running locally, then it will fetch all newly tokens from the local places
     *         whose the target type is LocalAsTarget(an only one instance from a static method of the class).
     *      2) the transition is sending message, then it will fetch newly tokens from both local places
     *         and communicating places. Here a target work for the communicating places, and you will get the
     *         newly tokens to be sent to the target, meanwhile, you will get newly tokens from local places
     *         with LocalAsTarget type.
     * </pre>
     *
     * @param target
     */
    public void checkNewlyTokens4Firing(ITarget target) {
        condition.getPlacePartition().forEach(partition -> {
            List<InputToken> availableTokens = new ArrayList<>();
            InputToken tokenSet = new InputToken();
            findAndSave(target, partition, tokenSet, availableTokens, 0);
            // Map<ITarget, Map<PlacePartition, List<InputToken>>> cache;
            cache.computeIfAbsent(target, obj -> new HashMap<>()).
                    computeIfAbsent(partition, obj -> new ArrayList<>()).addAll(availableTokens);
        });
    }

    private void findAndSave(ITarget target, PlacePartition partition, InputToken tokenSet, List<InputToken> availableTokens, int position) {
        List<Integer> pids = partition.getPids();
        if (position == pids.size()) {
            if (condition.test(partition, tokenSet)) {
                availableTokens.add(new InputToken(tokenSet));
            }
            return;
        }

        RuntimePlace place = inPlaces.get(pids.get(position));
        List<IToken> tokens = place.getNewlyTokens(target);
        for (int i = 0; i < tokens.size(); ++i) {
            tokenSet.addToken(pids.get(position), tokens.get(i));
            findAndSave(target, partition, tokenSet, availableTokens, position + 1);
            tokenSet.removeToken(pids.get(position));
        }
    }

    public boolean canFire() {
        enableTargets.clear();
        cache.forEach((target, partitions) -> {
            if (canFire(partitions)) {
                enableTargets.add(target);
            }
        });
        return !enableTargets.isEmpty();
    }

    private boolean canFire(Map<PlacePartition, List<InputToken>> partitions) {
        return partitions.values().stream().noneMatch(List::isEmpty);
    }

    public InputToken getRandmonInputToken() {
        ITarget target = enableTargets.get(random.nextInt(enableTargets.size()));
        Map<PlacePartition, List<InputToken>> partitions = cache.get(target);
        InputToken inputToken = new InputToken();
        partitions.values().forEach(inputTokens -> inputToken.merge(inputTokens.get(0)));

        return inputToken;
    }

    /**
     * @param inputTokens
     * @return
     */
    public OutputToken firing(InputToken inputTokens) {
        if (outputFunction == null) return null;
        removeTokenFromCache(inputTokens);
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
        if (targetPlaces.isEmpty()) {
            RuntimeIndividualCPN targetCPN = foldingCPN.getIndividualCPN((IOwner) target);
            RuntimePlace targetPlace = targetCPN.getPlace(pid);
            targetPlaces.put(pid, targetPlace);
        }
        return targetPlaces.get(pid);
    }

    public Map<ITarget, Map<PlacePartition, List<InputToken>>> getAllAvailableTokens() {
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
            ITarget target = token.getTarget();

            //Map<ITarget, Map<PlacePartition, List<InputToken>>> cache
            Map<PlacePartition, List<InputToken>> partitions = cache.get(target);
            for (Entry<PlacePartition, List<InputToken>> partitionEntry : partitions.entrySet()) {
                PlacePartition partition = partitionEntry.getKey();
                if (!partition.contains(pid)) continue;

                //HashMap<Integer, IToken>
                List<InputToken> tokenSets = partitionEntry.getValue();
                List<InputToken> removedTokenSets = tokenSets.stream().
                        filter(tokenSet -> tokenSet.containsValue(token)).collect(Collectors.toList());
                tokenSets.removeAll(removedTokenSets);
            }
        }
    }
}

/*
    public InputToken fire() {
        Map<PlacePartition, Integer> selectedTokens = new HashMap<>();
        cache.forEach((placeSet, tokenSets) -> {
            selectedTokens.put(placeSet, random.nextInt() % tokenSets.size());
        });
        return this.fire(selectedTokens);
    }

    public InputToken fire(Map<PlacePartition, Integer> selectedTokens) {
        // random get a tokenSet from each partition, and merge into one tokenSet.
        // return it and remove from cache including all tokens relative to tokenSet.
        // then remove tokens of this tokenSet from these origin places.
        List<InputToken> randomTokens = new ArrayList<>();
        cache.forEach((placeSet, tokenSets) -> {
            InputToken temp = tokenSets.get(selectedTokens.get(placeSet));
            randomTokens.add(temp);
            //TODO remove chosen tokens.
        });
        return InputToken.combine(randomTokens);
    }
 */
