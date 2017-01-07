package cn.edu.thu.jcpn.core.container;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class Storage implements IContainer {

    private int id;
    private String name;

    private Map<INode, List<IToken>> initTokens;
    private BiPredicate<IToken, IToken> replaceStrategy;

    private Storage() {
    }

    public Storage(int id, String name) {
        this();
        this.id = id;
        this.name = name;
        initTokens = new HashMap<>();
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

    public void addInitTokens(INode owner, Collection<IToken> tokens) {
        tokens.forEach(token -> addInitToken(owner, token));
    }

    public void addInitToken(INode owner, IToken token) {
        token.setOwner(owner);
        initTokens.computeIfAbsent(owner, obj -> new ArrayList<>()).add(token);
    }

    public BiPredicate<IToken, IToken> getReplaceStrategy() {
        return replaceStrategy;
    }

    /**
     *
     * @param replaceStrategy a pair of tokens, the left is the token that you want to added,
     *                        the right is the token that existed in the Storage.
     */
    public void setReplaceStrategy(BiPredicate<IToken, IToken> replaceStrategy) {
        this.replaceStrategy = replaceStrategy;
    }

    public List<IToken> getTokensByOwner(INode owner) {
        return initTokens.get(owner);
    }
}
