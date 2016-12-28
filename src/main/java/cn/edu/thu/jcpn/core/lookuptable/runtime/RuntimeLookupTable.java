package cn.edu.thu.jcpn.core.lookuptable.runtime;

import cn.edu.thu.jcpn.core.lookuptable.LookupTable;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by leven on 2016/12/24.
 */
public class RuntimeLookupTable {

    private int id;
    private String name;
    protected INode owner;

    private Map<String, String> properties;

    public RuntimeLookupTable(INode owner, LookupTable lookupTable) {
        this.owner = owner;
        this.id = lookupTable.getId();
        this.name = lookupTable.getName();

        this.properties = lookupTable.getPropertiesByOwner(this.owner);
        if (null == properties) properties = new HashMap<>();
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

    public Map<String, String> getProperties() {
        return properties;
    }

    public RuntimeLookupTable addProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
        return this;
    }

    public RuntimeLookupTable addProperty(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    public RuntimeLookupTable updateProperty(String key, String value) {
        this.addProperty(key, value);
        return this;
    }

    public String removeProperty(String key) {
        return properties.remove(key);
    }
}
