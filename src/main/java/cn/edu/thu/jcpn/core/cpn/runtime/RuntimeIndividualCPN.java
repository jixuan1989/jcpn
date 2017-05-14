package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.container.IContainer;
import cn.edu.thu.jcpn.core.container.runtime.IRuntimeContainer;
import cn.edu.thu.jcpn.core.container.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.container.Storage;
import cn.edu.thu.jcpn.core.container.runtime.RuntimeStorage;
import cn.edu.thu.jcpn.core.executor.transition.condition.ContainerPartition;
import cn.edu.thu.jcpn.core.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.executor.IRuntimeExecutor;
import cn.edu.thu.jcpn.core.monitor.IStorageMonitor;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.container.Place;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.executor.recoverer.Recoverer;
import cn.edu.thu.jcpn.core.executor.transition.Transition;
import cn.edu.thu.jcpn.core.executor.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;
import cn.edu.thu.jcpn.core.executor.recoverer.runtime.RuntimeRecoverer;
import cn.edu.thu.jcpn.core.executor.transition.runtime.RuntimeTransition;

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
    private Map<Integer, IRuntimeContainer> containers;
    private Map<Integer, IPlaceMonitor> placeMonitors;
    private Map<Integer, IStorageMonitor> storageMonitors;

    private Map<Integer, RuntimeTransition> transitions;
    private Map<Integer, ITransitionMonitor> transitionMonitors;

    private Map<Integer, RuntimeRecoverer> recoverers;

    /**
     * enable transition order by priority.
     */
    private Map<Integer, List<RuntimeTransition>> priorityTransitions;

    private List<RuntimeRecoverer> enableRecoverers;

    private RuntimeFoldingCPN foldingCPN;

    private boolean mode;

    private GlobalClock globalClock = GlobalClock.getInstance();

    RuntimeIndividualCPN(INode owner, RuntimeFoldingCPN foldingCPN, boolean mode) {
        this.owner = owner;
        this.containers = new HashMap<>();
        this.transitions = new HashMap<>();

        this.placeMonitors = new HashMap<>();
        this.storageMonitors = new HashMap<>();
        this.transitionMonitors = new HashMap<>();

        this.recoverers = new HashMap<>();

        this.foldingCPN = foldingCPN;

        this.mode = mode;
    }

    public INode getOwner() {
        return owner;
    }

    public Map<Integer, IRuntimeContainer> getContainers() {
        return containers;
    }

    public Map<Integer, RuntimeTransition> getTransitions() {
        return transitions;
    }

    public IRuntimeContainer getContainer(Integer cid) {
        return this.containers.get(cid);
    }

    public IPlaceMonitor getPlaceMonitor(int pid) {
        return placeMonitors.get(pid);
    }

    public IStorageMonitor getStorageMonitor(int sid) {
        return storageMonitors.get(sid);
    }

    public ITransitionMonitor getTransitionMonitor(int tid) {
        return transitionMonitors.get(tid);
    }

    private void addContainer(IContainer container) {
        if (container instanceof Place) {
            Place place = (Place) container;
            containers.put(place.getId(), new RuntimePlace(owner, place));
        } else {
            Storage storage = (Storage) container;
            containers.put(storage.getId(), new RuntimeStorage(owner, storage));
        }
    }

    public void addMonitor(int pid, IPlaceMonitor monitor) {
        if (!containers.containsKey(pid)) return;

        placeMonitors.put(pid, monitor);
    }

    public void addMonitor(int sid, IStorageMonitor monitor) {
        if (!containers.containsKey(sid)) return;

        storageMonitors.put(sid, monitor);
    }


    private void addTransition(Transition transition) {
        transitions.put(transition.getId(), new RuntimeTransition(owner, transition, containers, mode));
    }

    public void addMonitor(int tid, ITransitionMonitor monitor) {
        if (!transitions.containsKey(tid)) return;

        transitionMonitors.put(tid, monitor);
    }

    private void addRecoverer(Recoverer recoverer) {
        recoverers.put(recoverer.getId(), new RuntimeRecoverer(owner, recoverer, containers));
    }

    /**
     * copy place and transition into this cpn instance.  Initial tokens are not included.
     * <br> Though global place have been included in place, we extract them into this instance again
     *
     * @param transitions
     */
    public void construct(Collection<IContainer> containers, Collection<Transition> transitions,
                          Collection<Recoverer> recoverers) {
        //copy all the place and transition first.
        containers.forEach(this::addContainer);
        transitions.forEach(this::addTransition);
        recoverers.forEach(this::addRecoverer);
    }

    /**
     * neaten places: for each container, reassign the tokens according their time.
     * For future tokens, assign them to the newly tokens if their time are arrived.
     * For tested tokens, assign them to the timeout tokens if their time are timeout.
     */
    public void neatenContainers() {
        for (IRuntimeContainer container : containers.values()) {
            int cid = container.getId();
            List<IToken> timeoutTokens = container.reassignTokens();

            if (!timeoutTokens.isEmpty()) {
                transitions.values().forEach(transition -> transition.removeTokenFromCache(cid, timeoutTokens));
            }
        }
    }

    /**
     * process all the newly tokens and update transition' cached, then mark these tokens as tested.
     * this method is idempotent
     */
    public void notifyTransitions() {
        transitions.values().forEach(RuntimeTransition::checkNewlyTokens4Firing);
        containers.values().forEach(IRuntimeContainer::markTokensAsTested);

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


    public OutputToken fire(RuntimeTransition transition) {
        InputToken inputToken = transition.getInputToken();
        removeFromPlaces(inputToken);
        removeFromTransitions(inputToken);
        reportAfterTokenConsumed(globalClock.getTime(), inputToken, transition);

        OutputToken outputToken = transition.firing(inputToken);
        reportAfterFired(transition, inputToken, outputToken);
        reportAfterTokensAdded(outputToken, transition);
        return outputToken;
    }

    private void removeFromPlaces(InputToken inputToken) {
        inputToken.forEach((pid, token) -> {
            if (containers.get(pid) instanceof RuntimePlace) {
                RuntimePlace place = (RuntimePlace) containers.get(pid);
                place.removeTokenFromTest(token);
            }
        });
    }

    private void removeFromTransitions(InputToken inputToken) {
        transitions.values().forEach(innerTransition -> innerTransition.removeTokenFromCache(inputToken));
    }

    public boolean hasEnableRecoverers() {
        enableRecoverers = getEnableRecoverers();
        return !enableRecoverers.isEmpty();
    }

    private List<RuntimeRecoverer> getEnableRecoverers() {
        return recoverers.values().stream().
                filter(RuntimeRecoverer::canRun).collect(Collectors.toList());
    }

    public void fireAllRecoverers() {
        enableRecoverers.forEach(recoverer -> {
            Map<IToken, Map<Integer, List<IToken>>> tokenToPidTokens = recoverer.execute();
            tokenToPidTokens.forEach((token, pidTokens) ->
                    pidTokens.forEach((pid, tokens) -> reportAfterTokensAdded(pid, tokens, recoverer))
            );
        });
    }

    private void reportAfterTokenConsumed(long time, InputToken inputToken, RuntimeTransition transition) {
        inputToken.forEach((pid, token) -> this.reportAfterTokenConsumed(time, pid, token, transition));
    }

    private void reportAfterTokenConsumed(long time, int pid, IToken token, RuntimeTransition transition) {
        if (!placeMonitors.containsKey(pid)) return;

        IPlaceMonitor monitor = placeMonitors.get(pid);
        RuntimePlace place = (RuntimePlace) containers.get(pid);
        monitor.reportAfterTokenConsumed(time, owner, pid, place.getName(), token, transition.getId(),
                transition.getName(), place.getTimeoutTokens(), place.getTestedTokens(), place.getNewlyTokens(),
                place.getFutureTokens());

        Map<TokenType, Map<INode, Collection<IToken>>> pidAllTokens = getPidAllTokens(pid);
        monitor.reportAfterTokenConsumed(time, owner, pid, place.getName(), token, pidAllTokens.get(TIMEOUT),
                pidAllTokens.get(TESTED), pidAllTokens.get(NEWLY), pidAllTokens.get(FUTURE));
    }

    private void reportAfterTokensAdded(OutputToken outputToken, IRuntimeExecutor executor) {
        outputToken.forEach((to, pidTokens) ->
                pidTokens.forEach((pid, tokens) -> {
                    RuntimeIndividualCPN toCPN = foldingCPN.getIndividualCPN(to);
                    toCPN.reportAfterTokensAdded(pid, tokens, executor);
                })
        );
    }

    private void reportAfterTokensAdded(int id, List<IToken> tokens, IRuntimeExecutor executor) {
        if (!placeMonitors.containsKey(id) && !storageMonitors.containsKey(id)) return;

        if (placeMonitors.containsKey(id)) {
            IPlaceMonitor monitor = placeMonitors.get(id);
            RuntimePlace place = (RuntimePlace) containers.get(id);
            monitor.reportAfterTokensAdded(owner, place.getId(), place.getName(),
                    tokens, executor.getOwner(), executor.getId(), executor.getName(), place.getTimeoutTokens(),
                    place.getTestedTokens(), place.getNewlyTokens(), place.getFutureTokens());

            Map<TokenType, Map<INode, Collection<IToken>>> pidAllTokens = getPidAllTokens(id);
            monitor.reportAfterTokensAdded(owner, place.getId(), place.getName(), tokens, pidAllTokens.get(TIMEOUT),
                    pidAllTokens.get(TESTED), pidAllTokens.get(NEWLY), pidAllTokens.get(FUTURE));
        }
        else {
            IStorageMonitor monitor = storageMonitors.get(id);
            RuntimeStorage storage = (RuntimeStorage) containers.get(id);
            tokens.forEach(token -> monitor.reportAfterTokensAdded(token.getTime(), owner, storage.getName(), token));
        }
    }


    private void reportAfterFired(RuntimeTransition transition, InputToken inputToken, OutputToken outputToken) {
        if (!transitionMonitors.containsKey(transition.getId())) return;

        Map<ContainerPartition, List<InputToken>> cache = transition.getCache();

        ITransitionMonitor monitor = transitionMonitors.get(transition.getId());
        monitor.reportWhenFiring(globalClock.getTime(), owner, transition.getId(), transition.getName(), inputToken, outputToken);
        monitor.reportWhenFiring(inputToken, cache);
    }

    private Map<TokenType, Map<INode, Collection<IToken>>> getPidAllTokens(int pid) {
        Map<TokenType, Map<INode, Collection<IToken>>> res = new HashMap<>();
        Map<INode, Collection<IToken>> timeout = new HashMap<>();
        Map<INode, Collection<IToken>> tested = new HashMap<>();
        Map<INode, Collection<IToken>> newly = new HashMap<>();
        Map<INode, Collection<IToken>> future = new HashMap<>();

        foldingCPN.getNodeIndividualCPNs().forEach((node, individualCPN) -> {
            if (null != individualCPN.getContainer(pid) &&
                    individualCPN.getContainer(pid) instanceof RuntimePlace) {
                RuntimePlace runtimePlace = (RuntimePlace) foldingCPN.getIndividualCPN(owner).getContainer(pid);
                timeout.put(owner, runtimePlace.getTimeoutTokens());
                tested.put(owner, runtimePlace.getTestedTokens());
                newly.put(owner, runtimePlace.getNewlyTokens());
                future.put(owner, runtimePlace.getFutureTokens());
            }
        });

        res.put(TIMEOUT, timeout);
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
        return "ICPN [owner=" + owner + ", container=" + containers + ", transition=" + transitions + "]";
    }
}
