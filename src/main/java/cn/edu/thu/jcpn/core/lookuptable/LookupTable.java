package cn.edu.thu.jcpn.core.lookuptable;

import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by leven on 2016/12/23.
 */
public class LookupTable {

    private int id;
    private String name;

    /**
     * <owner, properties for this owner>
     */
    private Map<IOwner, Map<String, String>> initProperties;

    public LookupTable() {
        initProperties = new HashMap<>();
    }

    public LookupTable(int id) {
        this();
        this.id = id;
    }

    public LookupTable(int id, String name) {
        this(id);
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

    public Map<IOwner, Map<String, String>> getInitProperties() {
        return initProperties;
    }

    public void setInitProperties(Map<IOwner, Map<String, String>> initProperties) {
        this.initProperties = initProperties;
    }

    public void addInitProperties(IOwner owner, Map<String, String> properties) {
        initProperties.put(owner, properties);
    }

    public void addInitProperty(String key, String value) {
        initProperties.keySet().forEach(owner -> addInitProperty(owner, key, value));
    }

    public LookupTable addInitProperty(IOwner owner, String key, String value) {
        initProperties.computeIfAbsent(owner, obj -> new HashMap<>()).put(key, value);
        return this;
    }

    public Map<String, String> getPropertiesByOwner(IOwner owner) {
        return initProperties.get(owner);
    }
}
