package cn.edu.thu.jcpn.core.container.storage;

import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.*;
import java.util.function.Predicate;

public class RuntimeStorage {

    private int id;
    private String name;
    protected INode owner;

    private Predicate<IToken> replaceStrategy;

    private List<IToken> availableTokens;
    private List<IToken> futureTokens;

    private GlobalClock globalClock;

    public RuntimeStorage(INode owner, Storage storage) {
        this.owner = owner;
        this.id = storage.getId();
        this.name = storage.getName();

        this.replaceStrategy = storage.getReplaceStrategy();

        this.availableTokens = new ArrayList<>();
        this.futureTokens = new ArrayList<>();

        globalClock = GlobalClock.getInstance();
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public INode getOwner() {
        return owner;
    }

    public List<IToken> getAvailableTokens() {
        return availableTokens;
    }

    public List<IToken> getFutureTokens() {
        return futureTokens;
    }

    public Predicate<IToken> getReplaceStrategy() {
        return replaceStrategy;
    }

    public void addTokens(List<IToken> tokens) {
        if (null == tokens) return;

        tokens.forEach(this::addToken);
    }

    public void addToken(IToken token) {
        if (null == token) return;

        if (token.getTime() > globalClock.getTime()) {
            futureTokens.add(token);
        } else {
            availableTokens.add(token);
        }
    }
}