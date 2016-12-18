package cn.edu.thu.jcpn.core.transitions.runtime;

import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.places.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.ITarget;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.runtime.tokens.NullTarget;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.condition.Condition;
import cn.edu.thu.jcpn.core.transitions.condition.InputToken;
import cn.edu.thu.jcpn.core.transitions.condition.OutputToken;
import cn.edu.thu.jcpn.core.transitions.condition.PlacePartition;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RuntimeTransition {

    protected int id;
    protected String name;
    protected IOwner owner;
    protected int priority;

    protected Condition condition;
    protected Map<Integer, RuntimePlace> inPlaces;
    protected Map<ITarget, Map<Integer, RuntimePlace>> outPlaces;

    protected Map<ITarget, Map<PlacePartition, List<InputToken>>> cache;

    protected GlobalClock globalClock;
    protected static Random random = new Random();

    protected Function<InputToken, OutputToken> outputFunction;


    public RuntimeTransition(IOwner owner, Transition transition) {
        this.owner = owner;
        id = transition.getId();
        name = transition.getName();
        priority = transition.getPriority();
        condition = transition.getCondition();
        outputFunction = transition.getOutputFunction();
        
        globalClock = GlobalClock.getInstance();
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
     *
     */
    public void checkNewlyTokens4Firing() {
        outPlaces.keySet().forEach(this::checkNewlyTokens4Firing);
    }

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

    /**
     * remove all the candidate bindings related with the given token.
     * <br> this method needs to be called if you find that you can not get the token any more from the input place, while the token is still in cached bindings.
     * <br> that is to say, you should get a token firstly and then remove the token from cache immediately using this function.
     * <br> (only multi-threads mode requires )
     *
     * @param inputTokens
     */
    public void removeTokenFromCache(InputToken inputTokens) {
        for (Entry<Integer, IToken> inputToken : inputTokens.entrySet()) {

            int pid = inputToken.getKey();
            IToken token = inputToken.getValue();
            ITarget target = token.getTarget();

            Map<PlacePartition, List<InputToken>> partitions = cache.get(target);
            for (Entry<PlacePartition, List<InputToken>> partitionEntry : partitions.entrySet()) {
                PlacePartition partition = partitionEntry.getKey();
                if (!partition.contains(pid)) continue;

                List<InputToken> tokenSets = partitionEntry.getValue();
                List<InputToken> removedTokenSets = tokenSets.stream().
                        filter(tokenSet -> tokenSet.containsKey(pid)).collect(Collectors.toList());
                tokenSets.removeAll(removedTokenSets);
            }
        }
    }

    //TODO canfire must be finished.
    public boolean canFire() {
        for (Entry<ITarget, Map<PlacePartition, List<InputToken>>> targetEntry : cache.entrySet()) {
            ITarget target = targetEntry.getKey();
            Map<PlacePartition, List<InputToken>> partitions = targetEntry.getValue();
            for (Entry<PlacePartition, List<InputToken>> partitionEntry : partitions.entrySet()) {
                PlacePartition partition = partitionEntry.getKey();
                List<InputToken> tokens = partitionEntry.getValue();
                if (tokens.isEmpty()) {
                    break;
                }
            }
        }
        return true;
    }

    public OutputToken firing(InputToken inputTokens) {
        if (outputFunction == null) return null;
        removeTokenFromCache(inputTokens);
        OutputToken outputTokens = this.outputFunction.apply(inputTokens);

        for (Entry<ITarget, Map<Integer, List<IToken>>> targetPidTokens : outputTokens.entrySet()) {
            ITarget target = targetPidTokens.getKey();
            for (Entry<Integer, List<IToken>> pidTokens : targetPidTokens.getValue().entrySet()) {
                int pid = pidTokens.getKey();
                List<IToken> tokens = pidTokens.getValue();
                Map<Integer, RuntimePlace> places = outPlaces.get(target);
                RuntimePlace place = places.get(pid);
                ITarget outputTarget = tokens.get(0).getTarget();
                place.addTokens(outputTarget, tokens);

                long time = tokens.get(0).getTime();
                if (target instanceof NullTarget) {
                    globalClock.addAbsoluteTimepointForRunning(owner, time);
                }
                else {
                    globalClock.addAbsoluteTimepointForSending(target, time);
                }
            }
        }
        return outputTokens;
    }

    public Map<ITarget, Map<PlacePartition, List<InputToken>>> getAllAvailableTokens() {
        return cache;
    }
}
