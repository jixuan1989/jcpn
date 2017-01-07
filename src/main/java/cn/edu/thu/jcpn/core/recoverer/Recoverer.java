package cn.edu.thu.jcpn.core.recoverer;

import cn.edu.thu.jcpn.core.container.place.Place;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Recoverer {

    private int id;
    private String name;

    private Place inPlace;
    private Map<Integer, Place> outPlaces;

    private Function<IToken, Map<Integer, List<IToken>>> transferFunction;

    public Recoverer(int id, String name) {
        this.id = id;
        this.name = name;
        outPlaces = new HashMap<>();
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

    public Place getInPlace() {
        return inPlace;
    }

    public void setInPlace(Place place) {
        this.inPlace = inPlace;
    }

    public Map<Integer, Place> getOutPlaces() {
        return outPlaces;
    }

    public Recoverer addOutPlace(Place place) {
        outPlaces.put(place.getId(), place);
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
