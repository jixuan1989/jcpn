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
    }

    public Integer getId() {
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

    public INode getOwner() {
        return owner;
    }

    public void setOwner(INode owner) {
        this.owner = owner;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public RuntimeLookupTable addProperty(String key, String value) {
        if (null == properties) properties = new HashMap<>();

        properties.put(key, value);
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
