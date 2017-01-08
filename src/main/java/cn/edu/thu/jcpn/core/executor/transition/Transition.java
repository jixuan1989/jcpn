package cn.edu.thu.jcpn.core.executor.transition;

import cn.edu.thu.jcpn.core.container.IContainer;
import cn.edu.thu.jcpn.core.container.Place;
import cn.edu.thu.jcpn.core.executor.transition.condition.Condition;
import cn.edu.thu.jcpn.core.executor.transition.condition.ContainerPartition;
import cn.edu.thu.jcpn.core.executor.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static cn.edu.thu.jcpn.core.executor.transition.Transition.TransitionType.LOCAL;

/**
 * Transition has two type: local and transmit. The default type is LOCAL.
 */
public class Transition {

    private static int count = 0;

    private int id;
    private String name;
    private int priority = 500;
    private TransitionType type;

    private Condition condition;
    private Function<InputToken, OutputToken> transferFunction;

    /**
     * The tokens in some places obey FIFO.
     * If two places have FIFO stragety and both them are input places, we are not sure which one  is the first-class citizen.
     * <br>
     * For example, place p1 has tokens  [t1, t2] (t1 is in the head of the queue); and place p2 has tokens [t3, t4].
     * If all the compositions are: (t1, t4) and (t2, t3) which satisfy the condition in the transition, it is obvious
     * that both of them violate the FIFO strategy. Therefore, we have to define whose FIFO is more important to break the deadlock.
     */
    private Map<Integer, Integer> inCidPriorities;
    private Map<Integer, IContainer> inContainers;
    private Map<Integer, IContainer> outContainers;

    public enum TransitionType {
        LOCAL, TRANSMIT
    }

    private Transition() {
        this.id = count++;
        this.type = LOCAL;
        inCidPriorities = new HashMap<>();
        inContainers = new HashMap<>();
        outContainers = new HashMap<>();
        condition = new Condition();
    }

    private Transition(String name) {
        this();
        this.name = name;
    }

    public Transition(String name, TransitionType type) {
        this(name);
        this.type = type;
    }

    public Transition(String name, TransitionType type, int priority) {
        this(name, type);
        this.priority = priority;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
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

    public Map<Integer, Integer> getInCidPriorities() {
        return inCidPriorities;
    }

    public Map<Integer, IContainer> getInContainers() {
        return inContainers;
    }

    public Transition addInContainer(IContainer container) {
        return addInContainer(container, 500);
    }

    public Transition addInContainer(IContainer container, int priority) {
        inContainers.put(container.getId(), container);
        inCidPriorities.put(container.getId(), priority);
        return this;
    }

    public Map<Integer, IContainer> getOutContainers() {
        return outContainers;
    }

    public Transition addOutContainer(IContainer container) {
        outContainers.put(container.getId(), container);
        return this;
    }

    /**
     * @param transferFunction notice the time cost is relative time rather than absolute time
     */
    public void setTransferFunction(Function<InputToken, OutputToken> transferFunction) {
        this.transferFunction = transferFunction;
    }

    public Function<InputToken, OutputToken> getTransferFunction() {
        return transferFunction;
    }

    public Condition getCondition() {
        return condition;
    }
    public void addCondition(Predicate<InputToken> predicate, IContainer... containers) {
        ContainerPartition partition = new ContainerPartition();
        Arrays.stream(containers).forEach(container -> partition.add(container.getId()));
        condition.addPredicate(partition, predicate);
    }
}
