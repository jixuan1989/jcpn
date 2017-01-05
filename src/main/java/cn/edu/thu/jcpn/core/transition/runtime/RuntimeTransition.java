package cn.edu.thu.jcpn.core.transition.runtime;

import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeIndividualCPN;
import cn.edu.thu.jcpn.core.monitor.IExecutor;
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
 * hasEnableTransitions method to get the transitions who can be executed. Then the CPN randomly
 * picks one transition to prepare to execute it through calling the transition's getInputToken,
 * and gets the inputToken from the transition's cache. Then, the inputToken is used to fire the transition.
 * After that, the relative input tokens are removed from all relative transitions' caches and the original places.
 */
public class RuntimeTransition implements IExecutor {

    protected INode owner;
    private int id;
    private String name;
    private int priority = 500;
    private TransitionType type;

    private Condition condition;
    private Function<InputToken, OutputToken> transferFunction;

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

    public RuntimeTransition(INode owner, Transition transition, Map<Integer, RuntimePlace>
            runtimePlaces, RuntimeFoldingCPN foldingCPN) {
        this.owner = owner;
        this.id = transition.getId();
        this.name = transition.getName();
        this.type = transition.getType();
        this.transferFunction = transition.getTransferFunction();
        this.priority = transition.getPriority();
        this.condition = transition.getCondition();

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

    public int getId() {
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

    public Function<InputToken, OutputToken> getTransferFunction() {
        return transferFunction;
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

    public InputToken getInputToken() {
        InputToken inputToken = new InputToken();
        if (canFire()) {
            cache.values().forEach(partitionTokens -> inputToken.merge(partitionTokens.get(0)));
        }

        return inputToken;
    }

    /**
     * @param inputToken
     * @return
     */
    public OutputToken firing(InputToken inputToken) {
        if (null == transferFunction || inputToken.isEmpty()) return null;
        OutputToken outputToken = this.transferFunction.apply(inputToken);

        for (Entry<INode, Map<Integer, List<IToken>>> toPidTokens : outputToken.entrySet()) {
            INode to = toPidTokens.getKey();

            for (Entry<Integer, List<IToken>> pidTokens : toPidTokens.getValue().entrySet()) {
                int pid = pidTokens.getKey();
                List<IToken> tokens = pidTokens.getValue();
                tokens.forEach(token -> token.setOwner(to));
                RuntimePlace toPlace = getOutPlace(to, pid);
                toPlace.addTokens(tokens);
                registerEvents(owner, to, tokens);
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

    private void registerEvents(INode owner, INode to, List<IToken> tokens) {
        tokens.forEach(token -> {
            // register a event in the timeline.
            long time = token.getTime();
            if (owner.equals(to)) {
                globalClock.addAbsoluteTimePointForLocalHandle(owner, time);
            } else {
                globalClock.addAbsoluteTimePointForRemoteHandle(to, time);
            }
        });
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
        inputTokens.forEach(this::removeTokenFromCache);
    }

    public void removeTokenFromCache(int pid, List<IToken> tokens) {
        tokens.forEach(token -> removeTokenFromCache(pid, token));
    }

    private void removeTokenFromCache(int pid, IToken token) {
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