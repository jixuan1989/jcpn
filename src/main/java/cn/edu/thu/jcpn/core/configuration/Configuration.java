package cn.edu.thu.jcpn.core.configuration;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by leven on 2016/12/23.
 */
public class Configuration {

    private int id;
    private String name;

    /**
     * <owner, properties for this owner>
     */
    private Map<INode, Map<String, String>> initProperties;

    public Configuration() {
        initProperties = new HashMap<>();
    }

    public Configuration(int id) {
        this();
        this.id = id;
    }

    public Configuration(int id, String name) {
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

    public Map<INode, Map<String, String>> getInitProperties() {
        return initProperties;
    }

    public void setInitProperties(Map<INode, Map<String, String>> initProperties) {
        this.initProperties = initProperties;
    }

    public void addInitProperties(INode owner, Map<String, String> properties) {
        initProperties.put(owner, properties);
    }

    public void addInitProperty(String key, String value) {
        initProperties.keySet().forEach(owner -> addInitProperty(owner, key, value));
    }

    public Configuration addInitProperty(INode owner, String key, String value) {
        initProperties.computeIfAbsent(owner, obj -> new HashMap<>()).put(key, value);
        return this;
    }

    public Map<String, String> getPropertiesByOwner(INode owner) {
        return initProperties.get(owner);
    }
}
