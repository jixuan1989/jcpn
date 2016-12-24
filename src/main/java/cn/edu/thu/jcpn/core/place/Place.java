package cn.edu.thu.jcpn.core.place;

import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.edu.thu.jcpn.core.place.Place.PlaceStrategy.*;

public class Place {

    private int id;
    private String name;

    private PlaceStrategy strategy = BAG;

    /**
     * <owner, tokens for this owner>
     */
    private Map<IOwner, List<IToken>> initialTokens;

    /**
     * <br>BAG : random sort the tokens.
     * <br>FIFO : sort the tokens by their time, if exists tokens having same time, random sort this range tokens.
     */
    public enum PlaceStrategy {
        BAG, FIFO
    }

    public Place() {
        strategy = BAG;
        initialTokens = new HashMap<>();
    }

    public Place(int id) {
        this();
        this.id = id;
    }

    public Place(int id, String name) {
        this(id);
        this.name = name;
    }

    public PlaceStrategy getPlaceStrategy() {
        return strategy;
    }

    public void setPlaceStrategy(PlaceStrategy strategy) {
        if (this.strategy != strategy) {
            resetStrategy(strategy);
        }
        this.strategy = strategy;
    }

    private void resetStrategy(PlaceStrategy strategy) {
        //TODO firstly, we only support BAG.
        if (strategy == FIFO) {

        }
        else {

        }
    }

    public int getId() {
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

    public Map<IOwner, List<IToken>> getInitialTokens() {
        return initialTokens;
    }

    public void setInitialTokens(Map<IOwner, List<IToken>> initialTokens) {
        initialTokens.values().forEach(this::addInitTokens);
        initialTokens.clear();
    }

    public void addInitTokens(List<IToken> tokens) {
        tokens.forEach(this::addInitToken);
    }

    public void addInitToken(IToken token) {
        IOwner owner = token.getOwner();
        // 1) check whether the owner exists.
        // 2) add the token into the owner's token list.
        initialTokens.computeIfAbsent(owner, obj -> new ArrayList<>()).add(token);
    }

    public List<IToken> getTokensByOwner(IOwner owner) {
        return initialTokens.get(owner);
    }
}
