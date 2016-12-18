package cn.edu.thu.jcpn.core.places;

import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.ITarget;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.runtime.tokens.LocalAsTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.edu.thu.jcpn.core.places.Place.PlaceStrategy.*;
import static cn.edu.thu.jcpn.core.places.Place.PlaceType.*;

public class Place {

    private int id;
    private String name;
    private PlaceType type;

    private PlaceStrategy strategy = BAG;

    /**
     * <owner, target, tokens for this target>
     * <br>If target is LocalAsTarget, it means the token is used locally.
     * <br>In this case, the map contains only one <target, tokens> item.
     */
    private Map<IOwner, Map<ITarget, List<IToken>>> initialTokens;

    /**
     * <br>BAG : random sort the tokens.
     * <br>FIFO : sort the tokens by their time, if exists tokens having same time, random sort this range tokens.
     */
    public enum PlaceStrategy {
        BAG, FIFO
    }

    /**
     * <br>LOCAL : a place is local if it point to a local transitions.
     * <br>COMMUNICATING: a place is communicating if it point to a transmit transitions.
     */
    public enum PlaceType {
        LOCAL, COMMUNICATING
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

    public PlaceType getType() {
        return type;
    }

    public void setType(PlaceType type) {
        this.type = type;
    }

    public Map<IOwner, Map<ITarget, List<IToken>>> getInitialTokens() {
        return initialTokens;
    }

    public void setInitialTokens(Map<IOwner, Map<ITarget, List<IToken>>> initialTokens) {
        initialTokens.forEach((owner, targetTokens) ->
                targetTokens.forEach((target, tokens) ->
                        tokens.forEach(this::addInitToken)));
    }

    /**
     * TODO: IF PLACE TYPE IS SET, AND A DIFFERENT TYPE TOKEN IS ADDED, A ERROR OCCURS.
     *
     * @param token
     * @return
     */
    public Place addInitToken(IToken token) {
        IOwner owner = token.getOwner();
        ITarget target = token.getTarget();

        if (target instanceof LocalAsTarget)
            this.setType(LOCAL);
        else
            this.setType(COMMUNICATING);

        // 1) check whether the owner exists.
        // 2) check the target for the owner exists.
        // 3) add the token into the target's token list.
        initialTokens.computeIfAbsent(owner, obj -> new HashMap<>()).
                computeIfAbsent(target, obj -> new ArrayList<>()).add(token);
        return this;
    }

    public Map<ITarget, List<IToken>> getTokensByOwner(IOwner owner) {
        return initialTokens.get(owner);
    }
}
