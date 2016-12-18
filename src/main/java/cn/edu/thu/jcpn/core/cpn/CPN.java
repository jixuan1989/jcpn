package cn.edu.thu.jcpn.core.cpn;

import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.transitions.Transition;

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

    public void addPlace(int id, Place place) {
        places.put(id, place);
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

    public void addTransition(int id, Transition transition) {
        transitions.put(id, transition);
    }

    public Transition removeTransition(int id) {
        return transitions.remove(id);
    }
}
