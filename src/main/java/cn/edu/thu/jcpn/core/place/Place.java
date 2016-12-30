package cn.edu.thu.jcpn.core.place;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.edu.thu.jcpn.core.place.Place.PlaceStrategy.*;
import static cn.edu.thu.jcpn.core.place.Place.PlaceType.*;

public class Place {

    private int id;
    private String name;

    private PlaceType type;
    private PlaceStrategy strategy;

    /**
     * <owner, tokens for this owner>
     */
    private Map<INode, List<IToken>> initialTokens;

    /**
     * <br>BAG : random sort the tokens.
     * <br>FIFO : sort the tokens by their time, if exists tokens having same time, random sort this range tokens.
     */
    public enum PlaceStrategy {
        BAG, FIFO
    }

    public enum PlaceType {
        LOCAL, COMMUNICATING, CONSUMELESS
    }

    private Place() {
        type = LOCAL;
        strategy = BAG;
        initialTokens = new HashMap<>();
    }

    private Place(int id) {
        this();
        this.id = id;
    }

    public Place(int id, String name) {
        this(id);
        this.name = name;
    }

    public Place(int id, String name, PlaceType type) {
        this(id);
        this.name = name;
        this.type = type;
    }

    public PlaceStrategy getPlaceStrategy() {
        return strategy;
    }

    public void setPlaceStrategy(PlaceStrategy strategy) {
        this.strategy = strategy;
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

    public PlaceType getType() {
        return type;
    }

    public void setType(PlaceType type) {
        this.type = type;
    }

    public Map<INode, List<IToken>> getInitialTokens() {
        return initialTokens;
    }

    public void setInitialTokens(Map<INode, List<IToken>> initialTokens) {
        this.initialTokens = initialTokens;
    }

    public void addInitToken(INode owner, IToken token) {
        addInitToken(null, owner, null, token);
    }

    public void addInitToken(INode from, INode owner, INode to, IToken token) {
        token.setFrom(from);
        token.setOwner(owner);
        token.setTo(to);
        initialTokens.computeIfAbsent(owner, obj -> new ArrayList<>()).add(token);
    }

    public List<IToken> getTokensByOwner(INode owner) {
        return initialTokens.get(owner);
    }
}
