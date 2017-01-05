package cn.edu.thu.jcpn.core.recoverer.runtime;

import cn.edu.thu.jcpn.core.place.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.recoverer.Recoverer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by leven on 2017/1/4.
 */
public class RuntimeRecoverer {

    private int id;
    private String name;
    private INode owner;

    private RuntimePlace inPlace;
    private Map<Integer, RuntimePlace> outPlaces;

    private Function<IToken, Map<Integer, List<IToken>>> transferFunction;

    private GlobalClock globalClock;

    public RuntimeRecoverer(INode owner, Recoverer timeoutTransition,
                            Map<Integer, RuntimePlace> runtimePlaces) {
        this.owner = owner;
        this.id = timeoutTransition.getId();
        this.name = timeoutTransition.getName();

        int inPid = timeoutTransition.getInPlace().getId();
        this.inPlace = runtimePlaces.get(inPid);

        Set<Integer> outPids = timeoutTransition.getOutPlaces().keySet();
        this.outPlaces = new HashMap<>();
        outPids.forEach(pid -> this.outPlaces.put(pid, runtimePlaces.get(pid)));

        this.transferFunction = timeoutTransition.getTransferFunction();

        globalClock = GlobalClock.getInstance();
    }

    public boolean canRun() {
        return this.inPlace.hasTimeOutTokens();
    }

    /**
     * recover the timeout tokens and then clean up the timeout tokens' queue.
     */
    public void run() {
        List<IToken> timeouts = this.inPlace.getTimeoutTokens();
        timeouts.forEach(token -> {
            Map<Integer, List<IToken>> toPidTokens = transferFunction.apply(token);
            handleOutput(toPidTokens);
        });
        timeouts.clear();
    }

    /**
     * For each item of the toPidTokens, add the tokens to the specific place according
     * to the pid. Then register an event for each of the tokens.
     *
     * @param toPidTokens
     */
    private void handleOutput(Map<Integer, List<IToken>> toPidTokens) {
        toPidTokens.forEach((pid, tokens) -> {
            outPlaces.get(pid).addTokens(tokens);
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
