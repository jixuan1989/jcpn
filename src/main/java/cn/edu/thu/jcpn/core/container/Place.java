package cn.edu.thu.jcpn.core.container;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.edu.thu.jcpn.core.container.Place.PlaceType.*;

public class Place implements IContainer {

    private static int count = 1000;

    private int id;
    private String name;

    private PlaceType type;

    private IOnFireListener onFireListener;

    /**
     * <owner, tokens for this owner>
     */
    private Map<INode, List<IToken>> initTokens;

    public enum PlaceType {
        LOCAL, COMMUNICATING
    }

    private Place() {
        this.id = count++;
        type = LOCAL;
        initTokens = new HashMap<>();
    }

    private Place(String name) {
        this();
        this.name = name;
    }

    public Place(String name, PlaceType type) {
        this(name);
        this.type = type;
    }

    public int getId() {
        return id;
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

    public IOnFireListener getOnFireListener() {
        return onFireListener;
    }

    public void setOnFireListener(IOnFireListener onFireListener) {
        this.onFireListener = onFireListener;
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
