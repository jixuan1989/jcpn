package cn.edu.thu.jcpn.core.cpn.runtime;

import cn.edu.thu.jcpn.core.places.CommunicatingPlace;
import cn.edu.thu.jcpn.core.places.LocalPlace;
import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.places.runtime.*;
import cn.edu.thu.jcpn.core.transitions.LocalTransition;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.TransmitTransition;
import cn.edu.thu.jcpn.core.transitions.runtime.RuntimeLocalTransition;
import cn.edu.thu.jcpn.core.transitions.runtime.RuntimeTransition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static cn.edu.thu.jcpn.core.places.Place.PlaceType.COMMUNICATING;
import static cn.edu.thu.jcpn.core.places.Place.PlaceType.LOCAL;

public class RuntimeIndividualCPN {

    private static Logger logger = LogManager.getLogger();

    private static Random random = new Random();

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

    private void addRuntimePlace(LocalPlace place) {
        places.put(place.getId(), new RuntimeLocalPlace(place, this.owner));
    }

    private void addRuntimePlace(CommunicatingPlace place, Set<ITarget> targets) {
        places.put(place.getId(), new RuntimeCommunicatingPlace(place, this.owner, targets));
    }

    private void addRuntimeTransition(LocalTransition transition) {
        transitions.put(transition.getId(), new RuntimeLocalTransition(transition, owner));
    }

    private void addRuntimeTransition(TransmitTransition transition, Set<ITarget> targets) {
        transitions.put(transition.getId(), new RuntimeLocalTransition(transition, owner, targets));
    }

    /**
     * copy places and transitions into this cpn instance.  Initial tokens are not included.
     * <br> Though global places have been included in places, we extract them into this instance again
     *
     * @param places      no matter whether places have global places, we do not copy the global places in them
     * @param transitions
     * @param targets
     */
    public void construct(List<Place> places, List<Transition> transitions, Set<ITarget> targets) {
        //copy all the places and transitions first.
        places.stream().filter(place -> place.getType() == LOCAL).forEach(place -> addRuntimePlace((LocalPlace) place));
        places.stream().filter(place -> place.getType() == COMMUNICATING).forEach(place -> addRuntimePlace((CommunicatingPlace) place, targets));

        transitions.stream().filter(transition -> transition.getType() == LOCAL).forEach(transition -> addRuntime);
        if (!globalPlaces.isEmpty()) {
            globalPlaces.forEach(place -> this.addPlaceInstance(place));
        }

        if (transitions.containsKey(TransitionType.IndividualTransition)) {
            transitions.get(TransitionType.IndividualTransition).forEach
                    (transition -> this.addTransitionInstance((IndividualTransition) transition));
        }

        if (transitions.containsKey(TransitionType.ConnectionTransition)) {
            transitions.get(TransitionType.ConnectionTransition).forEach
                    (transition -> this.addTransitionInstance((ConnectionTransition) transition, otherIndividuals));
        }

        this.initPriorities();
    }

    /**
     * after construct the transitions, call this method to sort them according to the priority
     */
    private void initPriorities() {
        transitions.values().forEach
                (transition -> enablePriorTransitions.putIfAbsent(transition.getPriority(), new ArrayList<>()));
    }

    /**
     * process all the newly tokens and update transitions' cached, then mark these tokens as tested.
     * this method is idempotent
     */
    public void checkNewlyTokensAndMarkAsTested() {
        transitions.values().forEach(transition -> transition.checkNewlyTokens4Firing(this));
        places.values().forEach(place -> place.markTokensAsTested(this));
    }

    /**
     * remove selected tokens from the caches from all the transitions.
     *
     * @param binding
     */
    public void removeTokensFromAllTransitionsCache(MixedInputTokenBinding binding) {
        if (binding.hasSimpleTokens()) {
            binding.getLocalTokens().forEach((pid, token) -> {
                PlaceInstance place = places.get(pid);
                if (!place.getType().equals(PlaceType.GlobalPlace)) {
                    place.getOutArcs().keySet().forEach(tid ->
                            transitions.get(tid).removeTokenFromCache(place, token));
                }
            });
        }

        if (binding.hasConnectionTokens()) {
            binding.getConnectionTokens().forEach((pid, token) -> {
                PlaceInstance place = places.get(pid);
                place.getOutArcs().keySet().forEach(tid ->
                        transitions.get(tid).removeTokenFromCache(place, token, binding.getTarget()));
            });
        }
    }

    public IOutputTokenBinding fire(Integer tid, MixedInputTokenBinding binding) {
        return transitions.get(tid).firing(binding);
    }

    /**
     * after calling fire(), you must call this method because it will tell the clock the next time point.
     * <br>
     * this is not thread safe.
     * <br>
     * currently, this method is not thread safe, because ISimpleTokenBinding.addTokens is not thread safe.
     *
     * @param tokens
     */
    public void addLocalNewlyTokensAndScheduleNextTime(IOutputTokenBinding tokens) {
        logger.trace(() -> "handling a output token binding, " + tokens.getClass().getSimpleName());
        long time = GlobalClock.getInstance().getTime() + tokens.getLocalEffective();
        this.scheduleNextTimeForLocal(tokens, time);
        time = GlobalClock.getInstance().getTime() + tokens.getTargetEffective();
        this.scheduleNextTimeForRemote(tokens, time);
    }

    private void scheduleNextTimeForLocal(IOutputTokenBinding tokens, long absoluteTime) {
        if (tokens.hasLocalForIndividualPlace()) {
            tokens.getLocalForIndividualPlace().forEach((pid, list) ->
                    ((ILocalTokenOrganizator) places.get(pid)).addTokens(owner, list)
            );
            //TODO == 0?
            if (tokens.getLocalEffective() != 0) {
                logger.trace(() -> "ask for adding a new running event into the queue at time " + absoluteTime + " for owner " + owner);
                GlobalClock.getInstance().addAbsoluteTimepointForRunning(owner, absoluteTime);
            }
        }

        if (tokens.hasLocalForConnectionPlace()) {
            tokens.getLocalForConnectionPlace().forEach((pid, map) ->
                    map.forEach((target, list) -> ((ConnectionPlaceInstance) places.get(pid)).addTokens(target, list))
            );
            //TODO == 0?
            if (tokens.getLocalEffective() != 0) {
                logger.trace(() -> "ask for adding a new running event into the queue at time " + absoluteTime + " for owner " + owner);
                GlobalClock.getInstance().addAbsoluteTimepointForRunning(owner, absoluteTime);
            }
        }
    }

    private void scheduleNextTimeForRemote(IOutputTokenBinding tokens, long absoluteTime) {
        if (tokens.isRemote()) {
            remoteSchedule.computeIfAbsent(absoluteTime, target -> newRemoteScheduleMapAtOneTime());
            remoteSchedule.get(absoluteTime).computeIfAbsent(tokens.getTarget(), target -> newRemoteScheduleMapForOneTarget());
            Map<Integer, List<IColor>> map = remoteSchedule.get(absoluteTime).get(tokens.getTarget());
            tokens.getRemoteForIndividualPlace().forEach((pid, list) -> {
                map.computeIfPresent(pid, (p, pre) -> {
                    pre.addAll(list);
                    return pre;
                });
                map.computeIfAbsent(pid, p -> new ArrayList<>(list));
            });

            //tell clock the owner will send data to others
            if (tokens.getTargetEffective() == 0) {
                logger.debug("the time cost of a connection transition for sending data can not be 0, it will slow down the simulation");
            }

            logger.trace(() -> "ask for adding a new sending event into the queue at time " + absoluteTime + " for owner " + owner);
            GlobalClock.getInstance().addAbsoluteTimepointForSending(owner, absoluteTime);

            //tell clock who needs to be wake up to handle data.
            if (tokens.hasLocalForIndividualPlace() && tokens.getTargetEffective() != 0) {//TODO: whether do we need to add this condition?
                //FIXME: tokens.hasLocalForIndividualPlace and tokens.getTargetEffective may be wrong, because one is for local event and the other is for remote event.
                //please recheck it.
                logger.trace(() -> "ask for adding a new running event into the queue at time " + absoluteTime + " for owner " + owner);
                GlobalClock.getInstance().addAbsoluteTimepointForRunning(tokens.getTarget(), absoluteTime);
            }
        }
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
    public void addLocalNPNewlyTokens(Integer pid, List<IColor> tokens) {
        ILocalTokenOrganizator instance = (ILocalTokenOrganizator) places.get(pid);
        synchronized (instance) {
            instance.addTokens(owner, tokens);
        }
    }

    /**
     * send all the output tokens to other individual cpn instances.
     * <br> Then, to guarantee other programs have no error, we roughly add a time point in the running time line.
     *
     * @param absolutiveTime
     * @param connector
     */
    public void sendRemoteNewlyTokens(Long absolutiveTime, GlobalConnector connector) {
        if (logger.isDebugEnabled()) {
            remoteSchedule.get(absolutiveTime).forEach((target, tokens) -> logger.debug(() -> this.getOwner() + " sends " + tokens + " to " + target));
        }

        remoteSchedule.get(absolutiveTime).forEach((target, tokens) ->
                tokens.forEach((pid, list) -> connector.submitTokensToSomeone(target, pid, list))
        );

        //if sent, we must tell the timeline to running new data
        remoteSchedule.get(absolutiveTime).keySet().forEach(target ->
                GlobalClock.getInstance().addAbsoluteTimepointForRunning(target, absolutiveTime)
        );

        remoteSchedule.remove(absolutiveTime);
    }

    /**
     * this method is used for control the cpn instance manually.
     * <br> To guarantee other programs have no error, we roughly add a time point in the running time line.
     */
    public void sendRemoteNewlyTokens(Long absolutiveTime, ITarget target, GlobalConnector connector) {
        if (remoteSchedule.containsKey(absolutiveTime)) {
            remoteSchedule.get(absolutiveTime).get(target).forEach((pid, list) -> connector.submitTokensToSomeone(target, pid, list));
            GlobalClock.getInstance().addAbsoluteTimepointForRunning(target, absolutiveTime);
        }
    }

    /**
     * check which transitions can be fired
     *
     * @return
     */
    public boolean canFire() {
        List<TransitionInstance> canFireTransitions = this.transitions.values().stream().filter(t -> t.canFire(this)).collect(Collectors.toList());
        this.initPriorities();
        canFireTransitions.forEach(t -> enablePriorTransitions.get(t.getPriority()).add(t.getId()));

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
        return this.transitions.values().stream().noneMatch(transition -> transition.canFire(this));
    }


    /**
     * try to digest tokens from  places.
     * if success, the tokens will be put into binding, otherwise nothing happened
     * (But, the related places will be taken some tokens and then returned temp).
     * therefore, it is not a thread safe method.
     *
     * @param binding
     * @return
     */
    public boolean trytoGetTokensFromPlaces(MixedInputTokenBinding binding) {
        Map<Integer, IColor> simpleRemoved = new HashMap<>();

        if (binding.hasSimpleTokens()) {
            for (Map.Entry<Integer, IColor> entry : binding.getLocalTokens().entrySet()) {
                PlaceInstance placeInstance = places.get(entry.getKey());
                if (placeInstance.isInfiniteRead()) {//At least, global place is infiniteRead.
                    continue;
                }
                if (((ILocalTokenOrganizator) placeInstance).getTestedTokens().remove(entry.getValue())) {
                    simpleRemoved.put(entry.getKey(), entry.getValue());
                } else {
                    returnBack(simpleRemoved);
                    return false;
                }
            }
        }
        Map<Integer, IColor> connectionRemoved = new HashMap<>();
        if (binding.hasConnectionTokens()) {
            for (Map.Entry<Integer, IColor> entry : binding.getConnectionTokens().entrySet()) {
                PlaceInstance placeInstance = places.get(entry.getKey());
                if (placeInstance.isInfiniteRead()) {//At least, global place is infiniteRead.
                    continue;
                }
                if (((ConnectionPlaceInstance) placeInstance).getTestedTokens().get(binding.getTarget()) != null && ((ConnectionPlaceInstance) placeInstance).getTestedTokens().get(binding.getTarget()).remove(entry.getValue())) {
                    connectionRemoved.put(entry.getKey(), entry.getValue());
                } else {
                    returnBack(simpleRemoved);
                    returnBack(connectionRemoved, binding.getTarget());
                    return false;
                }
            }
        }
        return true;
    }

    private void returnBack(Map<Integer, IColor> removed) {
        for (Map.Entry<Integer, IColor> entry : removed.entrySet()) {
            PlaceInstance placeInstance = places.get(entry.getKey());
            if (placeInstance.getType().equals(PlaceType.GlobalPlace)) {
                //global place do not consume tokens
                logger.warn("global place should not be put into a removed map, please optimize your program");
                continue;
            }
            if (placeInstance.getPlaceStrategy().equals(PlaceStrategy.BAG)) {
                ((ILocalTokenOrganizator) placeInstance).getTestedTokens().add(entry.getValue());
            } else {
                ((ILocalTokenOrganizator) placeInstance).getTestedTokens().add(0, entry.getValue());
            }
        }
    }

    private void returnBack(Map<Integer, IColor> removed, ITarget target) {
        for (Map.Entry<Integer, IColor> entry : removed.entrySet()) {
            ConnectionPlaceInstance placeInstance = (ConnectionPlaceInstance) places.get(entry.getKey());
            placeInstance.getTestedTokens().computeIfAbsent(target, t -> new ArrayList<>());
            if (placeInstance.getPlaceStrategy().equals(PlaceStrategy.BAG)) {
                placeInstance.getTestedTokens().get(target).add(entry.getValue());
            } else {
                placeInstance.getTestedTokens().get(target).add(0, entry.getValue());
            }
        }
    }

    public String printAllTokens() {
        return "ICPN [owner=" + owner + ", places=" + places + "]";
    }

    public String printInterestingTokens(int[] pids) {
        Set<Integer> pidSet = new HashSet<>();
        Arrays.stream(pids).forEach(pid -> pidSet.add(pid));
        return "ICPN [owner=" + owner + ", places=" + places.values().stream().filter(p -> pidSet.contains(p.getId())).collect(Collectors.toList()) + "]";
    }

    @Override
    public String toString() {
        return "ICPN [owner=" + owner + ", places=" + places + ", transitions=" + transitions + "]";
    }

    public void logStatus() {
        places.forEach((owner, place) -> {
            place.logStatus();
        });
    }
}
