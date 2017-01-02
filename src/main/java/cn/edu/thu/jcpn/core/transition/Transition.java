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

/**
 * Transition has two type: local and transmit. The default type is LOCAL.
 */
public class Transition {

    private int id;
    private String name;
    private int priority = 500;
    private TransitionType type;

    private Condition condition;
    private Function<InputToken, OutputToken> outputFunction;

    /**
     * The tokens in some places obey FIFO.
     * If two places have FIFO stragety and both them are input places, we are not sure which one  is the first-class citizen.
     * <br>
     * For example, place p1 has tokens  [t1, t2] (t1 is in the head of the queue); and place p2 has tokens [t3, t4].
     * If all the compositions are: (t1, t4) and (t2, t3) which satisfy the condition in the transition, it is obvious
     * that both of them violate the FIFO strategy. Therefore, we have to define whose FIFO is more important to break the deadlock.
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
