package cn.edu.thu.jcpn.core.transition.condition;

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
     *       Map<PlacePartition, Map<PlacePartition, List<Predicate<InputToken>>>>
     *              /                    \
     *  a partition of pids   predicates relative to the subsets of these pids.
     * </pre>
     * note: for each key(placeSet), combination of placeSets in its value(map) makes the key.
     */
    protected Map<PlacePartition, Map<PlacePartition, List<Predicate<InputToken>>>> predicateItems;

    public Condition() {
        predicateItems = new HashMap<>();
    }

    public void addPredicate(PlacePartition partition, Predicate<InputToken> predicate) {
        List<PlacePartition> partitions = new ArrayList<>(); // partitions which needed to be combined.
        partition.forEach(pid ->
                predicateItems.keySet().forEach(placePartition -> {
                    if (placePartition.contains(pid)) {
                        partitions.add(placePartition);
                    }
                })
        );

        // key. all relative partitions combine into one partition.
        PlacePartition combinedPartition = PlacePartition.combine(partitions);
        // value. all predicates relative to these partitions.
        Map<PlacePartition, List<Predicate<InputToken>>> combinedPredicates = new HashMap<>();

        // for each partition to be combined, remove from the original map.
        // then combine multi-maps into one map.
        partitions.forEach(placePartition -> combinedPredicates.putAll(predicateItems.remove(placePartition)));
        predicateItems.put(combinedPartition, combinedPredicates);

        combinedPredicates.computeIfAbsent(partition.clone(), obj -> new ArrayList<>()).add(predicate);
    }

    /**
     * test if a tokenSet satisfy all the predicates.
     *
     * @param tokenSet
     * @return
     */
    public boolean test(PlacePartition partition, InputToken tokenSet) {
        if (predicateItems.get(partition) == null) {
            return true;
        }

        Map<PlacePartition, List<Predicate<InputToken>>> predicateItem = predicateItems.get(partition);

        for (Entry<PlacePartition, List<Predicate<InputToken>>> placeSetPredicates : predicateItem.entrySet()) {
            PlacePartition placeSet = placeSetPredicates.getKey();
            List<Predicate<InputToken>> predicates = placeSetPredicates.getValue();
            InputToken subTokenSet = tokenSet.getByPids(placeSet);
            for (Predicate<InputToken> predicate : predicates) {
                if (!predicate.test(subTokenSet)) {
                    return false;
                }
            }
        }

        return true;
    }

    public Collection<PlacePartition> getPlacePartition() {
        return predicateItems.keySet();
    }
}
