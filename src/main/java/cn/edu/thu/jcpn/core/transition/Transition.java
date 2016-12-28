package cn.edu.thu.jcpn.core.transition;

import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.transition.condition.Condition;
import cn.edu.thu.jcpn.core.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.transition.condition.OutputToken;
import cn.edu.thu.jcpn.core.transition.condition.PlacePartition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static cn.edu.thu.jcpn.core.transition.Transition.TransitionType.LOCAL;

public class Transition {

    private int id;
    private String name;
    private int priority = 500;
    private TransitionType type;

    private Condition condition;
    private Function<InputToken, OutputToken> outputFunction;

    /**
     * priorities for input places.
     */
    private Map<Integer, Integer> inPidPriorities;
    private Map<Integer, Place> inPlaces;
    private Map<Integer, Place> outPlaces;

    public enum TransitionType {
        LOCAL, TRANSMIT
    }

    public Transition() {
        this.type = LOCAL;
    }

    public Transition(int id, String name) {
        this();
        this.id = id;
        this.name = name;
        inPidPriorities = new HashMap<>();
        inPlaces = new HashMap<>();
        outPlaces = new HashMap<>();
        condition = new Condition();
    }

    public Transition(int id, String name, TransitionType type) {
        this(id, name);
        this.type = type;
    }

    public Transition(int id, String name, TransitionType type, int priority) {
        this(id, name, type);
        this.priority = priority;
    }

    public Map<Integer, Integer> getInPidPriorities() {
        return inPidPriorities;
    }

    public void setInPidPriorities(Map<Integer, Integer> inPidPriorities) {
        this.inPidPriorities = inPidPriorities;
    }

    public Map<Integer, Place> getInPlaces() {
        return inPlaces;
    }

    public void setInPlaces(Map<Integer, Place> inPlaces) {
        this.inPlaces = inPlaces;
    }

    public Transition addInPlace(Place place) {
        return addInPlace(place, 500);
    }

    public Transition addInPlace(Place place, int priority) {
        inPlaces.put(place.getId(), place);
        inPidPriorities.put(place.getId(), priority);
        return this;
    }

    public Map<Integer, Place> getOutPlaces() {
        return outPlaces;
    }

    public void setOutPlaces(Map<Integer, Place> outPlaces) {
        this.outPlaces = outPlaces;
    }

    public Transition addOutPlace(Place place) {
        outPlaces.put(place.getId(), place);
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
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
