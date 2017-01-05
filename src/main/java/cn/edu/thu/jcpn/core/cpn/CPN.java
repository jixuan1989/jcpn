package cn.edu.thu.jcpn.core.cpn;

import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.recoverer.Recoverer;
import cn.edu.thu.jcpn.core.transition.Transition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hxd on 16/5/14.
 */
public class CPN {

    private String version;
    private Map<Integer, Place> places;
    private Map<Integer, Transition> transitions;
    private Map<Integer, Recoverer> recoverers;

    public CPN() {
        super();
        this.places = new HashMap<>();
        this.transitions = new HashMap<>();
        this.recoverers = new HashMap<>();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<Integer, Place> getPlaces() {
        return places;
    }

    public void setPlaces(Map<Integer, Place> places) {
        this.places = places;
    }

    public Place getPlace(int placeId) {
        return places.get(placeId);
    }

    public void addPlace(Place place) {
        places.put(place.getId(), place);
    }

    public void addPlaces(Place... places) {
        Arrays.stream(places).forEach(this::addPlace);
    }

    public Place removePlace(int pid) {
        return places.remove(pid);
    }

    public Map<Integer, Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(Map<Integer, Transition> transitions) {
        this.transitions = transitions;
    }

    public void addTransition(Transition transition) {
        transitions.put(transition.getId(), transition);
    }

    public void addTransitions(Transition... transitions) {
        Arrays.stream(transitions).forEach(this::addTransition);
    }

    public Transition removeTransition(int tid) {
        return transitions.remove(tid);
    }

    public Map<Integer, Recoverer> getRecoverers() {
        return recoverers;
    }

    public void setRecoverers(Map<Integer, Recoverer> recoverers) {
        this.recoverers = recoverers;
    }

    public void addRecoverer(Recoverer recoverer) {
        recoverers.put(recoverer.getId(), recoverer);
    }

    public void addRecoverers(Recoverer... recoverers) {
        Arrays.stream(recoverers).forEach(this::addRecoverer);
    }

    public Recoverer removeRecoverer(int rid) {
        return recoverers.remove(rid);
    }
}
