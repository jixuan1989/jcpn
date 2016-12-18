package cn.edu.thu.jcpn.core.places.runtime;

import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.places.Place.*;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.ITarget;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class RuntimePlace {

    protected IOwner owner;
    protected int id;
    protected String name;
    protected PlaceType type;
    protected PlaceStrategy placeStrategy;

    protected Map<ITarget, List<IToken>> newlyTokens;
    protected Map<ITarget, List<IToken>> testedTokens;
    protected Map<ITarget, List<IToken>> futureTokens;

    protected GlobalClock globalClock;
    protected static Random random = new Random();

    public RuntimePlace(IOwner owner, Place place) {
        this.owner = owner;
        this.id = place.getId();
        this.name = place.getName();
        this.type = place.getType();
        this.placeStrategy = place.getPlaceStrategy();

        this.newlyTokens = new HashMap<>();
        this.testedTokens = new HashMap<>();
        this.futureTokens = new HashMap<>();
        this.globalClock = GlobalClock.getInstance();

        place.getTokensByOnwer(this.owner).forEach(this::addTokens);
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

    public PlaceStrategy getPlaceStrategy() {
        return placeStrategy;
    }

    public void setPlaceStrategy(PlaceStrategy placeStrategy) {
        this.placeStrategy = placeStrategy;
    }

    public PlaceType getType() {
        return type;
    }

    public void setType(PlaceType type) {
        this.type = type;
    }

    public IOwner getOwner() {
        return owner;
    }

    public void setOwner(IOwner owner) {
        this.owner = owner;
    }

    public Map<ITarget, List<IToken>> getNewlyTokens() {
        return newlyTokens;
    }

    public List<IToken> getNewlyTokens(ITarget target) {
        return newlyTokens.get(target);
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

    public void addTokens(Map<ITarget, List<IToken>> targetTokens) {
        targetTokens.forEach(this::addTokens);
    }

    public void addTokens(ITarget target, List<IToken> tokens) {
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

    /**
     * add a new token into existing tokens, order by their time.
     * If there exists multi tokens whose time are equal to the newly token,
     * random insert the newly token into them.
     *
     * @param tokens existing tokens
     * @param token new token that to be added
     */
    protected void addTokenByTimeOrder(List<IToken> tokens, IToken token) {
        int start = 0;
        int end = tokens.size() - 1;
        while (start + 1 < end) {
            int mid = start + (end - start) / 2;
            if (tokens.get(mid).getTime() < token.getTime()) {
                start = mid;
            }
            else { // tokens[mid].time >= token.time.
                end = mid;
            }
        }

        if (tokens.get(start).getTime() > token.getTime()) {
            tokens.add(start, token); // add(0, token);
            return;
        }
        else if (tokens.get(end).getTime() < token.getTime()) {
            tokens.add(end + 1, token); // add(tokens.length, token);
            return;
        }
        else if (tokens.get(start).getTime() < token.getTime() &&
                tokens.get(end).getTime() > token.getTime()) {
            tokens.add(end, token);
            return;
        }// otherwise, there exists some tokens that tokens.time equals to token.time.

        int insertStart = tokens.get(start).getTime() == token.getTime() ? start : end;

        start = insertStart;
        end = tokens.size() - 1;
        while (start + 1 < end) {
            int mid = start + (end - start) / 2;
            if (tokens.get(mid).getTime() <= token.getTime()) {
                start = mid;
            }
            else { // tokens[mid].time > token.time.
                end = mid;
            }
        }

        int insertEnd = (tokens.get(end).getTime() == token.getTime()) ? end : start;

        tokens.add(random.nextInt(insertEnd - insertStart + 1) + insertStart, token);
    }

    /**
     * move all the tokens from the newly queue to the test queue.
     */
    public void markTokensAsTested() {
        this.newlyTokens.forEach(this::markTokensAsTested);
    }

    public void markTokensAsTested(List<IToken> tokens) {
        tokens.forEach(this::markTokenAsTested);
    }

    /**
     * TODO if not exist ?
     * @param token
     */
    public void markTokenAsTested(IToken token) {
        ITarget target = token.getTarget();
        newlyTokens.computeIfAbsent(target, obj -> new ArrayList<>()).remove(token);
        testedTokens.computeIfAbsent(target, obj -> new ArrayList<>()).add(token);
    }

    public void markTokensAsTested(ITarget target, List<IToken> tokens) {
        tokens.forEach(this::markTokenAsTested);
    }

    /**
     * TODO if not exist ?
     * @param token
     */
    public void markTokenAsTested(ITarget target, IToken token) {
        newlyTokens.computeIfAbsent(target, obj -> new ArrayList<>()).remove(token);
        testedTokens.computeIfAbsent(target, obj -> new ArrayList<>()).add(token);
    }

    /**
     * check whether newly tokens is empty.
     * This method will check all the future tokens, and reassign tokens into newly or future queues, by comparing with global clock.
     * @return true if after reassigning, newly tokens is not empty;
     *         false, otherwise.
     */
    public boolean hasNewlyTokens() {
        futureTokens.forEach((target, tokens) -> {
            // collect all the tokens time is earlier than current time.
            List<IToken> timeUp = tokens.stream().filter(token -> token.getTime() <= GlobalClock.getInstance().getTime()).collect(Collectors.toList());
            tokens.removeAll(timeUp);
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
        testedTokens.getOrDefault(target, new ArrayList<>()).remove(token);
    }

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
    };
}
