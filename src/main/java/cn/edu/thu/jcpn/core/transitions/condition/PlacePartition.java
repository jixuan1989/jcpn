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
public class PlacePartition extends TreeSet<Integer> {

    public static PlacePartition combine(List<PlacePartition> partitions) {
        PlacePartition res = new PlacePartition();
        partitions.forEach(res::addAll);
        return res;
    }

    public List<Integer> getPids() {
        return new ArrayList<>(this);
    }

    public static PlacePartition intersect(Set<Integer> first, Set<Integer> second) {
        PlacePartition res = new PlacePartition();
        first.stream().filter(second::contains).forEach(res::add);
        return res;
    }

    public PlacePartition clone() {
        PlacePartition partition = new PlacePartition();
        this.forEach(partition::add);
        return partition;
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
