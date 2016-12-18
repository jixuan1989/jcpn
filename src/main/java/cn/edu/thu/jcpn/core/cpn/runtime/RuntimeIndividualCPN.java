package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.places.runtime.*;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.condition.InputToken;
import cn.edu.thu.jcpn.core.transitions.condition.OutputToken;
import cn.edu.thu.jcpn.core.transitions.runtime.RuntimeTransition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class RuntimeIndividualCPN {

    private static Logger logger = LogManager.getLogger();

    private static Random random = new Random();

    private GlobalClock globalClock = GlobalClock.getInstance();

    private IOwner owner;
    private Map<Integer, RuntimePlace> places;
    private Map<Integer, RuntimeTransition> transitions;

    public RuntimeIndividualCPN(IOwner owner) {
        this.owner = owner;
        places = new HashMap<>();
        transitions = new HashMap<>();
    }

    public IOwner getOwner() {
        return owner;
    }

    public void setOwner(IOwner owner) {
        this.owner = owner;
    }

    public Map<Integer, RuntimePlace> getPlaces() {
        return places;
    }

    public void setPlaces(Map<Integer, RuntimePlace> places) {
        this.places = places;
    }

    public Map<Integer, RuntimeTransition> getTransitions() {
        return transitions;
    }

    public void setTransitions(Map<Integer, RuntimeTransition> transitions) {
        this.transitions = transitions;
    }

    public RuntimePlace getPlace(Integer id) {
        return this.places.get(id);
    }

    private void addRuntimePlace(Place place) {
        places.put(place.getId(), new RuntimePlace(owner, place));
    }

    private void addRuntimeTransition(Transition transition) {
        transitions.put(transition.getId(), new RuntimeTransition(owner, transition));
    }

    /**
     * copy places and transitions into this cpn instance.  Initial tokens are not included.
     * <br> Though global places have been included in places, we extract them into this instance again
     *
     * @param places      no matter whether places have global places, we do not copy the global places in them
     * @param transitions
     */
    public void construct(Collection<Place> places, Collection<Transition> transitions) {
        //copy all the places and transitions first.
        places.forEach(this::addRuntimePlace);
        transitions.forEach(this::addRuntimeTransition);
    }

    /**
     * process all the newly tokens and update transitions' cached, then mark these tokens as tested.
     * this method is idempotent
     */
    public void checkNewlyTokensAndMarkAsTested() {
        transitions.values().forEach(RuntimeTransition::checkNewlyTokens4Firing);
        places.values().forEach(RuntimePlace::markTokensAsTested);
    }

    /**
     * remove selected tokens from the caches from all the transitions.
     *
     * @param binding
     */
    public void removeTokensFromAllTransitionsCache(InputToken inputTokens) {
        transitions.values().forEach(transition -> transition.removeTokenFromCache(inputTokens));
    }

    public OutputToken fire(Integer tid, InputToken inputTokens) {
        return transitions.get(tid).firing(inputTokens);
    }

    /**
     * this is thread safe.
     * <br>
     * only used for individual and global places
     * <br> this method do not register any events on the timeline.
     *
     * @param pid
     * @param tokens
     */
    public void addLocalNPNewlyTokens(Integer pid, List<IToken> tokens) {
        RuntimePlace instance = places.get(pid);
        synchronized (instance) {
            instance.addTokens(owner, tokens);
        }
    }

    /**
     * check which transitions can be fired
     *
     * @return
     */
    public boolean canFire() {
        List<RuntimeTransition> canFireTransitions = this.transitions.values().stream().filter(RuntimeTransition::canFire).collect(Collectors.toList());
        //TODO
        // canFireTransitions.forEach(t -> enablePriorTransitions.get(t.getPriority()).add(t.getId()));

        return canFireTransitions.size() > 0;
    }


    /**
     * meaningful only when asked canFire().<br>
     * this method is used for control cpn manually
     */
    public List<Integer> getCanFireTransitions() {
        for (Map.Entry<Integer, List<Integer>> entry : enablePriorTransitions.entrySet()) {
            if (entry.getValue().size() > 0) {
                return entry.getValue();
            }
        }
        return null;
    }


    /**
     * fire selected transition
     *
     * @param tid
     * @return
     */
    public MixedInputTokenBinding askForFire(Integer tid) {
        enablePriorTransitions = new HashMap<>(); // store all can fire tokens order by priority.
        if (this.transitions.get(tid).canFire(this)) {
            return this.transitions.get(tid).randomBinding(this);
        } else
            return null;
    }

    /**
     * randomly select a (possible can be fired) transition to fire
     *
     * @return null if there is no transitions can be fired.
     */
    public Integer getARandomTranstionWhichCanFire() {
        if (enablePriorTransitions.isEmpty()) canFire();

        List<Integer> prioirtyFire = getCanFireTransitions();
        if (prioirtyFire == null) {
            return null;
        }
        return prioirtyFire.get(random.nextInt(prioirtyFire.size()));
    }

    /**
     * re-check all the transitions to check whether there is a transition can be fired.
     *
     * @return
     */
    public boolean confirmNoTransitionCanFire() {
        return this.transitions.values().stream().noneMatch(transition -> transition.canFire());
    }

    @Override
    public String toString() {
        return "ICPN [owner=" + owner + ", places=" + places + ", transitions=" + transitions + "]";
    }

    public void logStatus() {
        places.values().forEach(RuntimePlace::logStatus);
    }
}
