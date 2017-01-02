package cn.edu.thu.jcpn.core.cpn;

import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.transition.Transition;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hxd on 16/5/14.
 */
public class CPN {

    private String version;
    private Map<Integer, Place> places;
    private Map<Integer, Transition> transitions;

    public CPN() {
        super();
        this.places = new HashMap<>();
        this.transitions = new HashMap<>();
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
        for(Place place : places){
            this.places.put(place.getId(),place);
        }
    }

    public Place removePlace(int id) {
        return places.remove(id);
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
        for(Transition transition : transitions){
            this.transitions.put(transition.getId(),transition);
        }
    }

    public Transition removeTransition(int id) {
        return transitions.remove(id);
    }
}
