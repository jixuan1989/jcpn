package cn.edu.thu.jcpn.core.storage.runtime;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.storage.Storage;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RuntimeStorage {

    private int id;
    private String name;
    protected INode owner;

    private Predicate<IToken> replaceStrategy;
    private Set<IToken> storage;

    public RuntimeStorage(INode owner, Storage storage) {
        this.owner = owner;
        this.id = storage.getId();
        this.name = storage.getName();

        this.replaceStrategy = storage.getReplaceStrategy();
        this.storage = new HashSet<>();
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

    public Set<IToken> getStorage() {
        return storage;
    }

    public void addTokens(List<IToken> tokens) {
        tokens.forEach(this::addToken);
    }

    public void addToken(IToken token) {
        if (replaceStrategy != null) {
            Set<IToken> removed = storage.stream().filter(replaceStrategy).collect(Collectors.toSet());
            storage.removeAll(removed);
        }
        storage.add(token);
    }

}
