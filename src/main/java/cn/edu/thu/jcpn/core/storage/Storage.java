package cn.edu.thu.jcpn.core.storage;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.function.Predicate;

public class Storage {

    private int id;
    private String name;

    private Predicate<IToken> replaceStrategy;

    private Storage() {
    }

    public Storage(int id, String name) {
        this();
        this.id = id;
        this.name = name;
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

    public Predicate<IToken> getReplaceStrategy() {
        return replaceStrategy;
    }

    public void setReplaceStrategy(Predicate<IToken> replaceStrategy) {
        this.replaceStrategy = replaceStrategy;
    }
}
