package cn.edu.thu.jcpn.core.executor.transition.condition;

import cn.edu.thu.jcpn.core.container.runtime.IRuntimeContainer;

import java.util.*;

/**
 * placeSet is a group of place' ids.
 * e.g., a group of input place, a group of output place.
 * <p>
 * Created by leven on 2016/12/7.
 */
public class ContainerPartition extends TreeSet<Integer> {

    public ContainerPartition() {
    }

    /**
     * cids is a copy of the data order by priorities of the places in these partition.
     */
    private List<Integer> cids = new ArrayList<>();

    public List<Integer> getCids() {
        return cids;
    }

    public void setPriorities(Map<Integer, Integer> priorities) {
        Map<Integer, List<Integer>> priorityCids = new TreeMap<>();
        this.forEach(cid ->
                priorityCids.computeIfAbsent(priorities.get(cid), obj -> new ArrayList<>()).add(cid));

        cids.clear(); //TODO fix.
        priorityCids.values().forEach(cids::addAll);
    }

    public ContainerPartition clone() {
        ContainerPartition partition = new ContainerPartition();
        this.forEach(partition::add);
        return partition;
    }

    public ContainerPartition subtract(ContainerPartition subPartition) {
        ContainerPartition partition = this.clone();
        partition.removeAll(subPartition);
        return partition;
    }

    public static ContainerPartition combine(Collection<ContainerPartition> partitions) {
        ContainerPartition res = new ContainerPartition();
        partitions.forEach(res::addAll);
        return res;
    }

    public static ContainerPartition generate(Collection<IRuntimeContainer> containers) {
        ContainerPartition res = new ContainerPartition();
        containers.forEach(container -> res.add(container.getId()));
        return res;
    }
}