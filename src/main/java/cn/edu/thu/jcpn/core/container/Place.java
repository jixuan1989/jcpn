package cn.edu.thu.jcpn.core.container;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.edu.thu.jcpn.core.container.Place.PlaceStrategy.*;
import static cn.edu.thu.jcpn.core.container.Place.PlaceType.*;

public class Place implements IContainer {

    private int id;
    private String name;

    private PlaceType type;
    private PlaceStrategy strategy;

    /**
     * <owner, tokens for this owner>
     */
    private Map<INode, List<IToken>> initTokens;

    /**
     * <br>BAG : random sort the tokens.
     * <br>FIFO : sort the tokens by their time, if exists tokens having same time, random sort this range tokens.
     */
    public enum PlaceStrategy {
        BAG, FIFO
    }

    public enum PlaceType {
        LOCAL, COMMUNICATING
    }

    private Place() {
        type = LOCAL;
        strategy = BAG;
        initTokens = new HashMap<>();
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

    public Map<INode, List<IToken>> getInitTokens() {
        return initTokens;
    }

    public void setInitTokens(Map<INode, List<IToken>> initTokens) {
        this.initTokens = initTokens;
    }

    public void addInitTokens(INode owner, List<IToken> tokens) {
        tokens.forEach(token -> addInitToken(null, owner, null, token));
    }

    public void addInitToken(INode owner, IToken token) {
        addInitToken(null, owner, null, token);
    }

    public void addInitToken(INode from, INode owner, INode to, IToken token) {
        token.setFrom(from);
        token.setOwner(owner);
        token.setTo(to);
        initTokens.computeIfAbsent(owner, obj -> new ArrayList<>()).add(token);
    }

    public List<IToken> getTokensByOwner(INode owner) {
        return initTokens.get(owner);
    }
}
