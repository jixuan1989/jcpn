package cn.edu.thu.jcpn.core.transitions.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * placeSet is a group of places' ids.
 * e.g., a group of input places, a group of output places.
 *
 * Created by leven on 2016/12/7.
 */
public class PlaceSet extends TreeSet<Integer> {

    public static PlaceSet combine(List<PlaceSet> placeSets) {
        PlaceSet res = new PlaceSet();
        placeSets.forEach(placeSet -> res.addAll(placeSet));
        return res;
    }

    public List<Integer> getPids() {
        return new ArrayList<Integer>(this);
    }

    public static PlaceSet intersect(Set<Integer> first, Set<Integer> second) {
        PlaceSet res = new PlaceSet();
        first.stream().filter(pid -> second.contains(pid)).forEach(pid -> res.add(pid));
        return res;
    }
}
