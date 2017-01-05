package cn.edu.thu.jcpn.core.transition.condition;

import cn.edu.thu.jcpn.core.place.runtime.RuntimePlace;

import java.util.*;

/**
 * placeSet is a group of place' ids.
 * e.g., a group of input place, a group of output place.
 * <p>
 * Created by leven on 2016/12/7.
 */
public class PlacePartition extends TreeSet<Integer> {

    public PlacePartition() {

    }

    public PlacePartition(Integer... pids) {
        Arrays.stream(pids).forEach(super::add);
    }

    /**
     * pids is a copy of the data order by priorities of the places in these partition.
     */
    private List<Integer> pids = new ArrayList<>();

    public List<Integer> getPids() {
        return pids;
    }

    public void setPriorities(Map<Integer, Integer> priorities) {
        Map<Integer, List<Integer>> priorityPids = new TreeMap<>();
        this.forEach(pid ->
                priorityPids.computeIfAbsent(priorities.get(pid), obj -> new ArrayList<>()).add(pid));

        priorityPids.values().forEach(pids::addAll);
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
}