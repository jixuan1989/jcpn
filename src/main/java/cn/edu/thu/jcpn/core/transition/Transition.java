package cn.edu.thu.jcpn.core.transition;

import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.transition.condition.Condition;
import cn.edu.thu.jcpn.core.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.transition.condition.OutputToken;
import cn.edu.thu.jcpn.core.transition.condition.PlacePartition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class Transition {

    private int id;
    private String name;
    private TransitionType type;

    private Condition condition;

    private Set<Place> inPlaces;
    private Set<Place> outPlaces;

    private Function<InputToken, OutputToken> outputFunction;

    public enum TransitionType {
        LOCAL, TRANSMIT
    }

    public Transition(int id, String name) {
        super();
        this.id = id;
        this.name = name;
        inPlaces = new HashSet<>();
        outPlaces = new HashSet<>();
        condition = new Condition();
    }

    public Set<Place> getInPlaces() {
        return inPlaces;
    }

    public void setInPlaces(Set<Place> inPlaces) {
        this.inPlaces = inPlaces;
    }

    public Transition addInPlace(Place place) {
        inPlaces.add(place);
        return this;
    }

    public Set<Place> getOutPlaces() {
        return outPlaces;
    }

    public void setOutPlaces(Set<Place> outPlaces) {
        this.outPlaces = outPlaces;
    }

    public Transition addOutPlace(Place place) {
        outPlaces.add(place);
        return this;
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

    public TransitionType getType() {
        return type;
    }

    public void setType(TransitionType type) {
        this.type = type;
    }

    /**
     * @param outputFunction notice the time cost is relative time rather than absolute time
     */
    public void setOutputFunction(Function<InputToken, OutputToken> outputFunction) {
        this.outputFunction = outputFunction;
    }

    public Function<InputToken, OutputToken> getOutputFunction() {
        return outputFunction;
    }

    public Condition getCondition() {
        return condition;
    }

    public void addCondition(PlacePartition placePartition, Predicate<InputToken> predicate) {
        condition.addPredicate(placePartition, predicate);
    }
}
