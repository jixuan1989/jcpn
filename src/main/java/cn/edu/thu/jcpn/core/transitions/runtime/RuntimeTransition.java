package cn.edu.thu.jcpn.core.transitions.runtime;

import cn.edu.thu.jcpn.core.places.runtime.IOwner;
import cn.edu.thu.jcpn.core.places.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.condition.Condition;
import cn.edu.thu.jcpn.core.transitions.condition.TokenSet;

import java.util.Map;
import java.util.Random;

public abstract class RuntimeTransition {

    protected int id;
    protected String name;
    protected IOwner owner;
    protected int priority;

    protected Condition condition;
    protected Map<Integer, RuntimePlace> inPlaces;
    protected Map<Integer, RuntimePlace> outPlaces;

    protected GlobalClock globalClock;
    protected static Random random = new Random();

    protected Function<TokenSet, IOutputTokenBinding> outputFunction;


    public RuntimeTransition(Transition transition, IOwner owner) {
        this.owner = owner;
        id = transition.getId();
        name = transition.getName();
        priority = transition.getPriority();
        condition = transition.getCondition();
        outputFunction = transition.getOutputFunction();
        
        globalClock = GlobalClock.getInstance();
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

    public IOwner getOwner() {
        return owner;
    }

    public void setOwner(IOwner owner) {
        this.owner = owner;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Map<Integer, RuntimePlace> getInPlaces() {
        return inPlaces;
    }

    public void setInPlaces(Map<Integer, RuntimePlace> inPlaces) {
        this.inPlaces = inPlaces;
    }

    public Map<Integer, RuntimePlace> getOutPlaces() {
        return outPlaces;
    }

    public void setOutPlaces(Map<Integer, RuntimePlace> outPlaces) {
        this.outPlaces = outPlaces;
    }

    public Function<TokenSet, IOutputTokenBinding> getOutputFunction() {
        return outputFunction;
    }

    public void setOutputFunction(Function<TokenSet, IOutputTokenBinding> outputFunction) {
        this.outputFunction = outputFunction;
    }

    /**
     * when new tokens enter its input places, which related to the transitions, this method needs to be called.
     * <br> the method will update its(transitions's) cachedBinding
     * <br> NOTICE: this class is not idempotent. That is to say, it assumes after checkNewlyTokens4Firing,
     * <br> all the newly tokens are moved into tested.
     * <br> (However, this method does not implement it, a CPNInstance class needs to control that)
     *
     */
    public abstract void checkNewlyTokens4Firing();

    /**
     * remove all the candidate bindings related with the given token.
     * <br> this method needs to be called if you find that you can not get the token any more from the input place, while the token is still in cached bindings.
     * <br> that is to say, you should get a token firstly and then remove the token from cache immediately using this function.
     * <br> (only multi-threads mode requires )
     *
     * @param tokenSet
     */
    public abstract void removeTokenFromCache(TokenSet tokenSet);

    public IOutputTokenBinding firing(TokenSet inputTokens) {
        if (outputFunction == null) return null;
        removeTokenFromCache(inputTokens);
        IOutputTokenBinding out = this.outputFunction.apply(inputTokens);
        modifyTokenTime(out);
        return out;
    }

    private void modifyTokenTime(IOutputTokenBinding tokens) {
        long time = GlobalClock.getInstance().getTime();
        if (tokens.hasLocalForIndividualPlace()) {
            tokens.getLocalForIndividualPlace().values().forEach(list -> list.forEach(token -> token.setTime(time + tokens.getLocalEffective())));
        }

        if (tokens.hasLocalForConnectionPlace()) {
            tokens.getLocalForConnectionPlace().values().forEach
                    (map -> map.values().forEach(list -> list.forEach(token -> token.setTime(time + tokens.getLocalEffective()))));
        }

        if (tokens.isRemote()) {
            tokens.getRemoteForIndividualPlace().values().forEach(list -> list.forEach(token -> token.setTime(time + tokens.getTargetEffective())));
        }
    }
}
