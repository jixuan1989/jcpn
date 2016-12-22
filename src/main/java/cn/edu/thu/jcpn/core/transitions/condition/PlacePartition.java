package cn.edu.thu.jcpn.core.transitions.condition;

import cn.edu.thu.jcpn.core.places.runtime.RuntimePlace;

import java.util.*;

/**
 * placeSet is a group of places' ids.
 * e.g., a group of input places, a group of output places.
 *
 * Created by leven on 2016/12/7.
 */
public class PlacePartition extends TreeSet<Integer> {

    public List<Integer> getPids() {
        return new ArrayList<>(this);
    }

    public PlacePartition clone() {
        PlacePartition partition = new PlacePartition();
        this.forEach(partition::add);
        return partition;
    }

    public PlacePartition subtract(PlacePartition subPartition) {
        PlacePartition partition = this.clone();
        partition.removeAll(subPartition);
        return partition;
    }

    public static PlacePartition combine(Collection<PlacePartition> partitions) {
        PlacePartition res = new PlacePartition();
        partitions.forEach(res::addAll);
        return res;
    }

    public static PlacePartition generate(Collection<RuntimePlace> places) {
        PlacePartition res = new PlacePartition();
        places.forEach(place -> res.add(place.getId()));
        return res;
    }

//    @Override
//    public boolean equals(Object otherPartition) {
//        if (otherPartition instanceof PlacePartition) {
//            return this.equals((PlacePartition) otherPartition);
//        }
//        return false;
//    }
//
//    public boolean equals(PlacePartition otherPartition) {
//        if (this.size() != otherPartition.size()) return false;
//
//        long size = this.size();
//        long thisContainOhter = this.stream().filter(this::contains).count();
//        long otherContainThis = this.stream().filter(otherPartition::contains).count();
//        return size == thisContainOhter && size == otherContainThis;
//    }
}
