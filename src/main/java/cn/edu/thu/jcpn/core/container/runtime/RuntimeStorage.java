package cn.edu.thu.jcpn.core.container.runtime;

import cn.edu.thu.jcpn.core.container.Storage;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RuntimeStorage implements IRuntimeContainer {

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

        this.globalClock = GlobalClock.getInstance();

        this.addTokens(storage.getTokensByOwner(this.owner));
    }

    public int getId() {
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

    public List<IToken> reassignTokens() {
        List<IToken> enables = futureTokens.stream().
                filter(token -> token.getTime() <= globalClock.getTime()).collect(Collectors.toList());
        futureTokens.removeAll(enables);
        availableTokens.addAll(enables);

        return new ArrayList<>();
    }

    public void logStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\tNewly:");
        if (availableTokens.size() > 0) {
            availableTokens.forEach(token -> sb.append("\t" + token.toString()));
        }
        sb.append("\n\t\tFuture:");
        if (futureTokens.size() > 0) {
            futureTokens.forEach(token -> sb.append("\t" + token.toString()));
        }
        System.out.println(String.format("\t%d: %s", id, getName()));
        System.out.println(sb.toString());
    }
}