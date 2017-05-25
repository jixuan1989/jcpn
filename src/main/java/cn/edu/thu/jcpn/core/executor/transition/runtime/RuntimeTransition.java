package cn.edu.thu.jcpn.core.executor.transition.runtime;

import cn.edu.thu.jcpn.core.container.IOnFireListener;
import cn.edu.thu.jcpn.core.container.runtime.IRuntimeContainer;
import cn.edu.thu.jcpn.core.container.runtime.InsertAgencyManager;
import cn.edu.thu.jcpn.core.executor.IRuntimeExecutor;
import cn.edu.thu.jcpn.core.executor.transition.condition.ContainerPartition;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.container.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.executor.transition.Transition;
import cn.edu.thu.jcpn.core.executor.transition.Transition.TransitionType;
import cn.edu.thu.jcpn.core.executor.transition.condition.Condition;
import cn.edu.thu.jcpn.core.executor.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;

import java.util.*;
import java.util.List;
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
public class RuntimeTransition implements IRuntimeExecutor {

    protected INode owner;
    private int id;
    private String name;
    private int priority = 500;
    private TransitionType type;

    private Condition condition;
    private Function<InputToken, OutputToken> transferFunction;

    private Map<Integer, Integer> inCidPriorities;
    private Map<Integer, IRuntimeContainer> inContainers;
    private Map<Integer, IRuntimeContainer> outContainers;

    private Map<ContainerPartition, List<InputToken>> cache;

    private InsertAgencyManager insertAgencyManager;

    private boolean mode;

    private GlobalClock globalClock;

    public RuntimeTransition(INode owner, Transition transition, Map<Integer, IRuntimeContainer> runtimeContainers, boolean mode) {
        this.owner = owner;
        this.id = transition.getId();
        this.name = transition.getName();
        this.type = transition.getType();
        this.transferFunction = transition.getTransferFunction();
        this.priority = transition.getPriority();
        this.condition = transition.getCondition();

        this.inCidPriorities = transition.getInCidPriorities();

        this.inContainers = new HashMap<>();
        transition.getInContainers().keySet().forEach(cid -> this.inContainers.put(cid, runtimeContainers.get(cid)));

        this.outContainers = new HashMap<>();
        transition.getOutContainers().keySet().forEach(cid -> this.outContainers.put(cid, runtimeContainers.get(cid)));

        initCache();

        this.mode = mode;

        this.globalClock = GlobalClock.getInstance();

        this.insertAgencyManager = InsertAgencyManager.getInstance();
    }

    private void initCache() {
        cache = new HashMap<>();
        Collection<ContainerPartition> partitions = condition.getContainerPartition();
        partitions.forEach(partition -> partition.setPriorities(inCidPriorities));
        partitions.forEach(partition -> cache.put(partition, new ArrayList<>()));
        ContainerPartition freePartition = getFreePartition();
        freePartition.forEach(cid -> {
            ContainerPartition partition = new ContainerPartition();
            partition.add(cid);
            partition.setPriorities(inCidPriorities);
            cache.put(partition, new ArrayList<>());
        });
    }

    private ContainerPartition getFreePartition() {
        ContainerPartition completePartition = ContainerPartition.generate(inContainers.values());
        ContainerPartition conditionPartition = ContainerPartition.combine(condition.getContainerPartition());
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

    public Map<Integer, IRuntimeContainer> getInContainers() {
        return inContainers;
    }

    public Map<Integer, IRuntimeContainer> getOutContainers() {
        return outContainers;
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
        // for each partition, get available tokens under the conditions.
        cache.keySet().forEach(partition -> {
            if (mode) {// auto mode, choose one available token from each place according to FIFO and priority.
                checkNewToken(partition);
            }
            else {
                List<InputToken> availableTokens = new ArrayList<>();
                InputToken tokenSet = new InputToken();

                findAndSave(partition, tokenSet, availableTokens, 0);
                cache.get(partition).addAll(availableTokens);
            }
            //checkNewTokens(partition); // iteration version.
        });
    }

    private void findAndSave(ContainerPartition partition, InputToken tokenSet, List<InputToken> availableTokens, int position) {
        List<Integer> cids = partition.getCids();
        if (position == cids.size()) {
            if (position == tokenSet.size() && condition.test(partition, tokenSet) && containNew(partition, tokenSet)) {
                availableTokens.add(new InputToken(tokenSet));
            }
            return;
        }

        int cid = cids.get(position);
        List<IToken> tokens = getTokens(cid);
        tokens.forEach(token -> {
            tokenSet.addToken(cid, token);
            findAndSave(partition, tokenSet, availableTokens, position + 1);
            tokenSet.removeToken(cid);
        });
//        for (int i = 0; i < tokens.size(); ++i) {
//            tokenSet.addToken(cids.get(position), tokens.get(i));
//            findAndSave(partition, tokenSet, availableTokens, position + 1);
//            tokenSet.removeToken(cids.get(position));
//        }
    }

    private List<IToken> getTokens(int cid) {
        IRuntimeContainer container = inContainers.get(cid);
        List<IToken> tokens = new ArrayList<>();

        tokens.addAll(container.getTestedTokens());
        if (container.hasNewlyTokens()) tokens.addAll(container.getNewlyTokens());

        if (container instanceof RuntimePlace) {
            RuntimePlace place = (RuntimePlace) container;
            if (place.hasOnFireListener()) {
                IOnFireListener onFireListener = place.getOnFireListener();
                onFireListener.modifyTokens(tokens);
            }
        }

        return tokens;
    }

    private boolean containNew(ContainerPartition partition, InputToken inputToken) {
        for (int cid : partition) {
            IRuntimeContainer container = inContainers.get(cid);
            List<IToken> tokens = container.getNewlyTokens();
            IToken token = inputToken.get(cid);
            if (tokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private void checkNewToken(ContainerPartition partition) {
        List<InputToken> partitionCache = cache.computeIfAbsent(partition, obj -> new ArrayList<>());
        if (partitionCache.size() > 0) return;

        List<Integer> cids = partition.getCids();
        Map<Integer, List<IToken>> cidTokens = new HashMap<>();
        cids.forEach(cid -> cidTokens.put(cid, getTokens(cid)));
        if (cidTokens.values().stream().anyMatch(tokens -> tokens.size() == 0)) return;

        Map<Integer, Integer> cidIndexes = new HashMap<>();
        cids.forEach(cid -> cidIndexes.put(cid, 0));

        while (notFinish(cidTokens, cidIndexes)) {
            InputToken inputToken = getInputToken(cidTokens, cidIndexes);
            if (condition.test(partition, inputToken) && containNew(partition, inputToken)) {
                partitionCache.add(inputToken);
                break;
            }

            increaseIndex(cids, cidTokens, cidIndexes);
        }

        if (partitionCache.size() > 0) return;

        InputToken inputToken = getInputToken(cidTokens, cidIndexes);
        if (condition.test(partition, inputToken) && containNew(partition, inputToken)) partitionCache.add(inputToken);
    }


    // Iteration version for check and generate cache content.
    private void checkNewTokens(ContainerPartition partition) {
        List<InputToken> partitionCache = cache.computeIfAbsent(partition, obj -> new ArrayList<>());

        List<Integer> cids = partition.getCids();
        Map<Integer, List<IToken>> cidTokens = new HashMap<>();
        cids.forEach(cid -> cidTokens.put(cid, getTokens(cid)));
        if (cidTokens.values().stream().anyMatch(tokens -> tokens.size() == 0)) return;

        Map<Integer, Integer> cidIndexes = new HashMap<>();
        cids.forEach(cid -> cidIndexes.put(cid, 0));

        while (notFinish(cidTokens, cidIndexes)) {
            InputToken inputToken = getInputToken(cidTokens, cidIndexes);
            if (condition.test(partition, inputToken) && containNew(partition, inputToken)) partitionCache.add(inputToken);

            increaseIndex(cids, cidTokens, cidIndexes);
        }

        InputToken inputToken = getInputToken(cidTokens, cidIndexes);
        if (condition.test(partition, inputToken) && containNew(partition, inputToken)) partitionCache.add(inputToken);
    }

    private InputToken getInputToken(Map<Integer, List<IToken>> cidTokens, Map<Integer, Integer> cidIndexes) {
        InputToken inputToken = new InputToken();
        cidIndexes.forEach((cid, index) -> inputToken.addToken(cid, cidTokens.get(cid).get(index)));
        return inputToken;
    }

    private boolean notFinish(Map<Integer, List<IToken>> cidTokens, Map<Integer, Integer> cidIndexes) {
        return cidIndexes.entrySet().stream().anyMatch(entry -> entry.getValue() + 1 < cidTokens.get(entry.getKey()).size());
    }

    private void increaseIndex(List<Integer> cids, Map<Integer, List<IToken>> cidTokens, Map<Integer, Integer> cidIndexes) {
        int count = 0;
        int cid = cids.get(count);
        int index = cidIndexes.get(cid);
        int size = cidTokens.get(cid).size();

        int newIndex = (index + 1) % size;
        cidIndexes.put(cid, newIndex);

        int carry = (index + 1) / size;
        while (carry != 0) {
            ++count;
            cid = cids.get(count);
            index = cidIndexes.get(cid);
            size = cidTokens.get(cid).size();

            newIndex = (index + 1) % size;
            cidIndexes.put(cid, newIndex);

            carry = (index + 1) / size;
        }
    }

    public boolean canFire() {
        return cache.values().stream().noneMatch(List::isEmpty);
    }

    // origin version.
    public InputToken getInputToken() {
        InputToken inputToken = new InputToken();
        if (!canFire()) return inputToken;

        cache.values().forEach(partitionTokens -> inputToken.merge(partitionTokens.get(0)));
        return inputToken;
    }

    /**
    public InputToken getInputToken(){// x is used for cassandra experiments..
        int x= Integer.valueOf(System.getProperty("x","0"));
        if(x<0){
            return getProbabilityInputToken(0-x);
        }else{
            return getDiscreteInputToken(x);
        }
    }

    // x < 1

    private InputToken getProbabilityInputToken(int x) {
        InputToken inputToken = new InputToken();
        if (!canFire()) return inputToken;

        Random random = new Random();

        cache.forEach(((partition, inputTokens) -> {
            int count = 0;

            int cid = -1;
            for (int partitionCid : partition.getCids()) {
                if (inContainers.get(partitionCid).getName().equals("writing queue"))  {
                    cid = partitionCid;
                }
            }

            if (cid != -1) {
                while (inputTokens.get(count).get(cid).isLocal() &&
                        count + 1 < inputTokens.size() && random.nextInt(1000) > x) {
                    ++count;
                }
            }

            inputToken.merge(inputTokens.get(count));
        }));

        return inputToken;
    }
    */


    // x > 1 and is discrete value.
    private Map<Long, Integer> moveDistance;
    private InputToken getDiscreteInputToken(int x) {
        InputToken inputToken = new InputToken();
        if (!canFire()) return inputToken;

        cache.forEach(((partition, inputTokens) -> {
            InputToken inputToken1 = inputTokens.get(0);

            int cid = -1;
            for (int partitionCid : partition.getCids()) {
                if (inContainers.get(partitionCid).getName().equals("writing queue"))  {
                    cid = partitionCid;
                }
            }

            if (cid != -1) {
                if (moveDistance == null) moveDistance = new HashMap<>();

                IToken token = inputToken1.get(cid);
                int distance = moveDistance.getOrDefault(token.getTime(), 0);
                while (token.isLocal() && distance < x && inputTokens.size() > 1) {
                    inputTokens.add(1, inputToken1);
                    moveDistance.put(token.getTime(), ++distance);
                    inputToken1 = inputTokens.get(0);
                    token = inputToken1.get(cid);
                    distance = moveDistance.getOrDefault(token.getTime(), 0);
                }
                moveDistance.remove(token.getTime());
            }

            inputToken.merge(inputToken1);
        }));

        return inputToken;
    }

    /**
     * @param inputToken
     * @return
     */
    public OutputToken firing(InputToken inputToken) {
        if (null == transferFunction || inputToken.isEmpty()) return null;
        OutputToken outputToken = transferFunction.apply(inputToken);

        for (Entry<INode, Map<Integer, List<IToken>>> toCidTokens : outputToken.entrySet()) {
            INode to = toCidTokens.getKey();
            Map<Integer, List<IToken>> cidTokens = toCidTokens.getValue();

            //cidTokens.values().forEach(tokens -> tokens.forEach(token -> token.setOwner(to)));
            for (List<IToken> tokens : cidTokens.values()) {
                for (IToken token: tokens) {
                    token.setOwner(to);
                }
            }

            if (to.equals(owner)) {
                cidTokens.forEach((cid, tokens) -> {
                    outContainers.get(cid).addTokens(tokens);
                    tokens.forEach(token -> globalClock.addAbsoluteTimePointForLocalHandle(owner, token.getTime()));
                });
            }
            else {
                cidTokens.forEach((cid, tokens) -> {
                    insertAgencyManager.addTokens(to, cid, tokens);
                    tokens.forEach(token -> globalClock.addAbsoluteTimePointForRemoteHandle(to, token.getTime()));
                    tokens.forEach(token -> globalClock.addAbsoluteTimePointForLocalHandle(to, token.getTime()));
                });
            }
        }

        return outputToken;
    }

    public Map<ContainerPartition, List<InputToken>> getAllAvailableTokens() {
        return cache;
    }

    public Map<ContainerPartition, List<InputToken>> getCache() {
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
        for (Entry<ContainerPartition, List<InputToken>> partitionEntry : cache.entrySet()) {
            ContainerPartition partition = partitionEntry.getKey();
            if (!partition.contains(pid) || inContainers.get(pid) == null ||
                    !(inContainers.get(pid) instanceof RuntimePlace)) continue;

            List<InputToken> tokenSets = partitionEntry.getValue();
            List<InputToken> removedTokenSets = tokenSets.stream().
                    filter(tokenSet -> tokenSet.containsValue(token)).collect(Collectors.toList());
            tokenSets.removeAll(removedTokenSets);
        }
    }
}