package cn.edu.thu.jcpn.core.container.runtime;

import cn.edu.thu.jcpn.core.container.Storage;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class RuntimeStorage implements IRuntimeContainer {

    private int id;
    private String name;
    protected INode owner;

    private BiPredicate<IToken, IToken> replaceStrategy;

    private List<IToken> testedTokens;
    private List<IToken> newlyTokens;
    private List<IToken> futureTokens;

    private GlobalClock globalClock;

    public RuntimeStorage(INode owner, Storage storage) {
        this.owner = owner;
        this.id = storage.getId();
        this.name = storage.getName();

        this.replaceStrategy = storage.getReplaceStrategy();

        this.testedTokens = new ArrayList<>();
        this.newlyTokens = new ArrayList<>();
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

    public List<IToken> getTestedTokens() {
        return testedTokens;
    }

    public List<IToken> getNewlyTokens() {
        return newlyTokens;
    }

    public boolean hasNewlyTokens() {
        return !newlyTokens.isEmpty();
    }

    public List<IToken> getFutureTokens() {
        return futureTokens;
    }

    public BiPredicate<IToken, IToken> getReplaceStrategy() {
        return replaceStrategy;
    }

    public void addTokens(List<IToken> tokens) {
        if (null == tokens) return;

        tokens.forEach(this::addToken);
    }

    private void addToken(IToken token) {
        if (null == token) return;

        // remove the tokens which are meet the replace function from tested queue and newly queue.
        if(replaceStrategy != null) {
            Collection<IToken> removed = testedTokens.stream().
                    filter(availableToken -> replaceStrategy.test(token, availableToken)).collect(Collectors.toList());
            testedTokens.removeAll(removed);

            removed = newlyTokens.stream().
                    filter(availableToken -> replaceStrategy.test(token, availableToken)).collect(Collectors.toList());
            newlyTokens.removeAll(removed);
        }

        if (token.getTime() > globalClock.getTime()) {
            futureTokens.add(token);
        } else {
            newlyTokens.add(token);
        }
    }

    public List<IToken> reassignTokens() {
        List<IToken> enables = futureTokens.stream().
                filter(token -> token.getTime() <= globalClock.getTime()).collect(Collectors.toList());
        futureTokens.removeAll(enables);
        newlyTokens.addAll(enables);

        return new ArrayList<>();
    }

    public void markTokensAsTested() {
        testedTokens.addAll(newlyTokens);
        newlyTokens.clear();
    }
}