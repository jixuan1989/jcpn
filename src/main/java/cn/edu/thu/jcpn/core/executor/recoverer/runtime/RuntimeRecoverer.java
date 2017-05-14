package cn.edu.thu.jcpn.core.executor.recoverer.runtime;

import cn.edu.thu.jcpn.core.container.runtime.IRuntimeContainer;
import cn.edu.thu.jcpn.core.executor.IRuntimeExecutor;
import cn.edu.thu.jcpn.core.container.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.executor.recoverer.Recoverer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by leven on 2017/1/4.
 */
public class RuntimeRecoverer implements IRuntimeExecutor {

    private int id;
    private String name;
    private INode owner;

    private RuntimePlace inPlace;
    private Map<Integer, IRuntimeContainer> outContainers;

    private Function<IToken, Map<Integer, List<IToken>>> transferFunction;

    private GlobalClock globalClock;

    public RuntimeRecoverer(INode owner, Recoverer recoverer,
                            Map<Integer, IRuntimeContainer> runtimeContainers) {
        this.owner = owner;
        this.id = recoverer.getId();
        this.name = recoverer.getName();

        int inPid = recoverer.getInPlace().getId();
        this.inPlace = (RuntimePlace) runtimeContainers.get(inPid);

        Set<Integer> outCids = recoverer.getOutContainers().keySet();
        this.outContainers = new HashMap<>();
        outCids.forEach(cid -> this.outContainers.put(cid, runtimeContainers.get(cid)));

        this.transferFunction = recoverer.getTransferFunction();

        this.globalClock = GlobalClock.getInstance();
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

    public boolean canRun() {
        return this.inPlace.hasTimeOutTokens();
    }

    /**
     * recover the timeout tokens and then clean up the timeout tokens' queue.
     */
    public Map<IToken, Map<Integer, List<IToken>>> execute() {
        Map<IToken, Map<Integer, List<IToken>>> tokenToCidTokens = new HashMap<>();
        List<IToken> timeouts = this.inPlace.getTimeoutTokens();
        timeouts.forEach(token -> {
            Map<Integer, List<IToken>> toCidTokens = transferFunction.apply(token);
            tokenToCidTokens.put(token, toCidTokens);
            handleOutput(toCidTokens);
        });
        timeouts.clear();

        return tokenToCidTokens;
    }

    /**
     * For each item of the toPidTokens, add the tokens to the specific place according
     * to the pid. Then register an event for each of the tokens.
     *
     * @param toCidTokens
     */
    private void handleOutput(Map<Integer, List<IToken>> toCidTokens) {
        toCidTokens.forEach((cid, tokens) -> {
            outContainers.get(cid).addTokens(tokens);
            tokens.forEach(this::registerEvents);
        });
    }

    /**
     * register events into the global timeline according to the newly token's time.
     *
     * @param token
     */
    private void registerEvents(IToken token) {
        long time = token.getTime();
        globalClock.addAbsoluteTimePointForLocalHandle(owner, time);
    }
}
