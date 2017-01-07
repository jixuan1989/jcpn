package cn.edu.thu.jcpn.core.executor.transition.condition;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * 1. partition predicates into several exclusive sets. i.e. combination of the placeSets.
 * 2. cache the partition predicates test result.
 * 3. once newly tokens of a place come in, update the specific cache.
 * 4. once fire a token, remove the token from all the caches and other transition caches.
 * <p>
 * Created by leven on 2016/12/7.
 */
public class Condition {

    /**
     * <pre>
     *       Map<ContainerPartition, Map<ContainerPartition, List<Predicate<InputToken>>>>
     *              /                    \
     *  a partition of pids   predicates relative to the subsets of these pids.
     * </pre>
     * note: for each key(placeSet), combination of placeSets in its value(map) makes the key.
     */
    protected Map<ContainerPartition, Map<ContainerPartition, List<Predicate<InputToken>>>> predicateItems;

    public Condition() {
        predicateItems = new HashMap<>();
    }

    public void addPredicate(ContainerPartition partition, Predicate<InputToken> predicate) {
        List<ContainerPartition> partitions = new ArrayList<>(); // partitions which needed to be combined.
        partition.forEach(pid ->
                predicateItems.keySet().forEach(containerPartition -> {
                    if (containerPartition.contains(pid)) {
                        partitions.add(containerPartition);
                    }
                })
        );
        partitions.add(partition);

        // key. all relative partitions combine into one partition.
        ContainerPartition combinedPartition = ContainerPartition.combine(partitions);
        // value. all predicates relative to these partitions.
        Map<ContainerPartition, List<Predicate<InputToken>>> combinedPredicates = new HashMap<>();

        // for each partition to be combined, remove from the original map.
        // then combine multi-maps into one map.
        partitions.forEach(containerPartition -> {
            Map<ContainerPartition, List<Predicate<InputToken>>> removed = predicateItems.remove(containerPartition);
            if (null != removed && !removed.isEmpty()) combinedPredicates.putAll(removed);
        });
        predicateItems.put(combinedPartition, combinedPredicates);

        combinedPredicates.computeIfAbsent(partition.clone(), obj -> new ArrayList<>()).add(predicate);
    }

    /**
     * test if a tokenSet satisfy all the predicates.
     *
     * @param inputToken
     * @return
     */
    public boolean test(ContainerPartition partition, InputToken inputToken) {
        if (predicateItems.get(partition) == null) {
            return true;
        }

        Map<ContainerPartition, List<Predicate<InputToken>>> predicateItem = predicateItems.get(partition);

        for (Entry<ContainerPartition, List<Predicate<InputToken>>> partitionPredicates : predicateItem.entrySet()) {
            ContainerPartition subPartition = partitionPredicates.getKey();
            List<Predicate<InputToken>> predicates = partitionPredicates.getValue();
            InputToken subInputToken = inputToken.getByCids(subPartition);
            for (Predicate<InputToken> predicate : predicates) {
                if (!predicate.test(subInputToken)) {
                    return false;
                }
            }
        }

        return true;
    }

    public Collection<ContainerPartition> getContainerPartition() {
        return predicateItems.keySet();
    }
}
