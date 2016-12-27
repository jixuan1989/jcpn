package cn.edu.thu.jcpn.core.place.runtime;

import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.place.Place.*;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class RuntimePlace {

    private int id;
    private String name;
    protected INode owner;

    private PlaceStrategy placeStrategy;

    /**
     * If it is a local place, then it only has a LocalAsTarget in the three tokenMaps.
     * <br>And the type of the place is LOCAL.
     * <br>Else, it has multi to entry, for each to, it may exists several tokens.
     * <br>And in this case, it does not have a LocalAsTarget entry.
     * <br>And the type of the place is COMMUNICATING.
     */
    private List<IToken> newlyTokens;
    private List<IToken> testedTokens;
    private List<IToken> futureTokens;

    private GlobalClock globalClock;
    private static Random random = new Random();

    public RuntimePlace(INode owner, Place place) {
        this.owner = owner;
        this.id = place.getId();
        this.name = place.getName();
        this.placeStrategy = place.getPlaceStrategy();

        this.newlyTokens = new ArrayList<>();
        this.testedTokens = new ArrayList<>();
        this.futureTokens = new CopyOnWriteArrayList<>();
        this.globalClock = GlobalClock.getInstance();

        this.addTokens(place.getTokensByOwner(this.owner));
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

    public INode getOwner() {
        return owner;
    }

    public void setOwner(INode owner) {
        this.owner = owner;
    }

    public List<IToken> getNewlyTokens() {
        return newlyTokens;
    }

    public void setNewlyTokens(List<IToken> newlyTokens) {
        this.newlyTokens = newlyTokens;
    }

    public List<IToken> getTestedTokens() {
        return testedTokens;
    }

    public void setTestedTokens(List<IToken> testedTokens) {
        this.testedTokens = testedTokens;
    }

    public List<IToken> getFutureTokens() {
        return futureTokens;
    }

    public void setFutureTokens(List<IToken> futureTokens) {
        this.futureTokens = futureTokens;
    }

    public void addTokens(List<IToken> tokens) {
        if (null == tokens) return;

        tokens.forEach(this::addToken);
    }

    public void addToken(IToken token) {
        if (null == token) return;

        if (this.getPlaceStrategy().equals(PlaceStrategy.BAG)) {
            addTokenBAG(token);
        }
        else {
            addTokenFIFO(token);
        }
    }

    private void addTokenBAG(IToken token) {
        if (token.getTime() > globalClock.getTime()) {
            int position = random.nextInt(futureTokens.size() + 1);
            futureTokens.add(position, token);
        }
        else {
            int position = random.nextInt(newlyTokens.size() + 1);
            newlyTokens.add(position, token);
        }
    }

    private void addTokenFIFO(IToken token) {
        if (token.getTime() > globalClock.getTime()) {
            addTokenByTimeOrder(futureTokens, token);
        }
        else {
            addTokenByTimeOrder(newlyTokens, token);
        }
    }

    /**
     * add a new token into existing tokens, order by their time.
     * If there exists multi tokens whose time are equal to the newly token,
     * random insert the newly token into them.
     * Use a binary-search algorithm here.
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
        testedTokens.addAll(newlyTokens);
        newlyTokens.clear();
    }

    /**
     * check whether newly tokens is empty.
     * This method will check all the future tokens, and reassign tokens into newly or future queues, by comparing with global clock.
     * @return true if after reassigning, newly tokens is not empty;
     *         false, otherwise.
     */
    public boolean hasNewlyTokens() {
        List<IToken> timeUp = futureTokens.stream().
                filter(token -> token.getTime() <= GlobalClock.getInstance().getTime()).collect(Collectors.toList());
        futureTokens.removeAll(timeUp);
        newlyTokens.addAll(timeUp);

        return !newlyTokens.isEmpty();
    }

    public boolean removeTokenFromTest(IToken token) {
        return testedTokens.remove(token);
    }

    public void logStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\tNewly:");
        if (newlyTokens.size() > 0) {
            newlyTokens.forEach(token -> sb.append("\t" + token.toString()));
        }
        sb.append("\n\t\tTested:");
        if (testedTokens.size() > 0) {
            testedTokens.forEach(token -> sb.append("\t" + token.toString()));
        }
        sb.append("\n\t\tFuture:");
        if (futureTokens.size() > 0) {
            futureTokens.forEach(token -> sb.append("\t" + token.toString()));
        }
        System.out.println(String.format("\t%d: %s", id, getName()));
        System.out.println(sb.toString());
    }
}
