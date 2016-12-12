package cn.edu.thu.jcpn.core.places.runtime;

import cn.edu.thu.jcpn.core.places.CommunicatingPlace;
import cn.edu.thu.jcpn.core.places.Place.*;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author hxd
 */
public class RuntimeCommunicatingPlace extends RuntimePlace {

    private static Logger logger = LogManager.getLogger();

    protected IOwner owner;
    protected Set<ITarget> targets;
    protected Map<ITarget, List<IToken>> newlyTokens;
    protected Map<ITarget, List<IToken>> testedTokens;
    protected Map<ITarget, List<IToken>> futureTokens;

    public RuntimeCommunicatingPlace(CommunicatingPlace place, IOwner owner, Map<ITarget, List<IToken>> tokens) {
        this(place, owner, tokens.keySet());
        addTokens(tokens);
    }

    public RuntimeCommunicatingPlace(CommunicatingPlace place, IOwner owner, Set<ITarget> targets) {
        super(place);
        this.owner = owner;
        newlyTokens = new HashMap<>();
        testedTokens = new HashMap<>();
        futureTokens = new HashMap<>();

        this.targets = new HashSet<>(targets);
        this.targets.remove(owner);
    }

    public IOwner getOwner() {
        return owner;
    }

    public void setOwner(IOwner owner) {
        this.owner = owner;
    }

    public Set<ITarget> getTargets() {
        return targets;
    }

    public void setTargets(Set<ITarget> targets) {
        this.targets = targets;
    }

    public Map<ITarget, List<IToken>> getNewlyTokens() {
        return newlyTokens;
    }

    public void setNewlyTokens(Map<ITarget, List<IToken>> newlyTokens) {
        this.newlyTokens = newlyTokens;
    }

    public Map<ITarget, List<IToken>> getTestedTokens() {
        return testedTokens;
    }

    public void setTestedTokens(Map<ITarget, List<IToken>> testedTokens) {
        this.testedTokens = testedTokens;
    }

    public Map<ITarget, List<IToken>> getFutureTokens() {
        return futureTokens;
    }

    public void setFutureTokens(Map<ITarget, List<IToken>> futureTokens) {
        this.futureTokens = futureTokens;
    }

    public void addTokens(Map<ITarget, List<IToken>> tokens) {
        tokens.entrySet().forEach(token -> this.addTokens(token.getKey(), token.getValue()));
    }

    public void addTokens(ITarget target, List<IToken> tokens) {
        if (!targets.contains(target)) targets.add(target);

        if (this.getPlaceStrategy().equals(PlaceStrategy.BAG)) {
            assignTokensIntoDifferentQueues(target, tokens,
                    (token, color) -> futureTokens.get(token).add(random.nextInt(futureTokens.get(token).size() + 1), color),
                    (token, color) -> newlyTokens.get(token).add(random.nextInt(newlyTokens.get(token).size() + 1), color));
        } else {
            assignTokensIntoDifferentQueues(target, tokens,
                    (token, color) -> addTokenByTimeOrder(futureTokens.get(token), color),
                    (token, color) -> addTokenByTimeOrder(newlyTokens.get(token), color));
        }
    }

    private void assignTokensIntoDifferentQueues(ITarget target, List<IToken> tokens, BiConsumer<ITarget, IToken> futureConsumer, BiConsumer<ITarget, IToken> newlyConsumer) {
        GlobalClock globalClock = GlobalClock.getInstance();
        tokens.forEach(color -> {
            if (color.getTime() > globalClock.getTime()) {
                futureTokens.putIfAbsent(target, new ArrayList<>());
                futureConsumer.accept(target, color);
            } else {
                newlyTokens.putIfAbsent(target, new ArrayList<>());
                newlyConsumer.accept(target, color);
            }
        });
    }

    @Override
    public void markTokensAsTested() {
        newlyTokens.forEach((target, tokens) ->
                testedTokens.computeIfAbsent(target, obj -> new ArrayList<>()).addAll(tokens));
        newlyTokens.clear();
    }

    @Override
    public boolean hasNewlyTokens() {
        futureTokens.forEach((target, list) -> {
            // collect all the tokens time is earlier than current time.
            List<IToken> timeUp = list.stream().filter(token -> token.getTime() <= GlobalClock.getInstance().getTime()).collect(Collectors.toList());
            list.removeAll(timeUp);
            newlyTokens.computeIfAbsent(target, obj -> new ArrayList<>()).addAll(timeUp);
        });

        for (List<IToken> targetNewlyTokens : newlyTokens.values()) {
            if (!targetNewlyTokens.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void removeTokenFromTest(ITarget target, IToken token) {
        testedTokens.get(target).remove(token);
    }

    @Override
    public void logStatus() {
        StringBuilder sb = new StringBuilder();
        if (newlyTokens.size() > 0) {
            newlyTokens.forEach((target, colorList) -> {
                sb.append(String.format("\ttarget : %s\n", target));
                colorList.forEach(color -> sb.append("\t" + color.toString() + " newly "));
            });
        }
        if (testedTokens.size() > 0) {
            sb.append("\n");
            testedTokens.forEach((target, colorList) -> {
                sb.append(String.format("\ttarget : %s\n", target));
                colorList.forEach(color -> sb.append("\t" + color.toString() + " tested "));
            });
        }
        if (futureTokens.size() > 0) {
            sb.append("\n");
            futureTokens.forEach((target, colorList) -> {
                sb.append(String.format("\ttarget : %s\n", target));
                colorList.forEach(color -> sb.append("\t" + color.toString() + " future "));
            });
        }
        System.out.println(String.format("place : %s", getName()));
        System.out.println(sb.toString());
    }
}
