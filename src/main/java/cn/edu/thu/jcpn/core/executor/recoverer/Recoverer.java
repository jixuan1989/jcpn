package cn.edu.thu.jcpn.core.executor.recoverer;

import cn.edu.thu.jcpn.core.container.IContainer;
import cn.edu.thu.jcpn.core.container.Place;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Recoverer {

    private static int count = 0;

    private int id;
    private String name;

    private Place inPlace;
    private Map<Integer, IContainer> outContainers;

    private Function<IToken, Map<Integer, List<IToken>>> transferFunction;

    public Recoverer(String name) {
        this.id = count++;
        this.name = name;
        outContainers = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Place getInPlace() {
        return inPlace;
    }

    public void addInPlace(Place place) {
        this.inPlace = place;
    }

    public Map<Integer, IContainer> getOutContainers() {
        return outContainers;
    }

    public Recoverer addOutContainer(IContainer container) {
        outContainers.put(container.getId(), container);
        return this;
    }

    /**
     * @param transferFunction notice the time cost is relative time rather than absolute time
     */
    public void setTransferFunction(Function<IToken, Map<Integer, List<IToken>>> transferFunction) {
        this.transferFunction = transferFunction;
    }

    public Function<IToken, Map<Integer, List<IToken>>> getTransferFunction() {
        return transferFunction;
    }
}
