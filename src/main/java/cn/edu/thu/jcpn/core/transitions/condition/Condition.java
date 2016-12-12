package cn.edu.thu.jcpn.core.transitions.condition;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * 1. partition predicates into several exclusive sets. i.e. combination of the placeSets.
 * 2. cache the partition predicates test result.
 * 3. once newly tokens of a place come in, update the specific cache.
 * 4. once fire a token, remove the token from all the caches and other transitions caches.
 *
 * Created by leven on 2016/12/7.
 */
public class Condition {

    /**
     * <pre>
     *       Map<PlaceSet, Map<PlaceSet, List<Predicate<TokenSet>>>>
     *              /                    \
     *  a partition of pids   predicates relative to the subsets of these pids.
     * </pre>
     * note: for each key(placeSet), combination of placeSets in its value(map) makes the key.
     */
    protected Map<PlaceSet, Map<PlaceSet, List<Predicate<TokenSet>>>> predicateItems;

    public Condition() {
        predicateItems = new HashMap<>();
    }

    public void addPredicate(PlaceSet placeSet, Predicate<TokenSet> predicate) {

        List<PlaceSet> partitions = new ArrayList<>(); // partitions which needed to be combined.
        placeSet.forEach(pid ->
                predicateItems.keySet().forEach(partition -> {
                    if (partition.contains(pid)) {
                        partitions.add(partition);
                    }
                })
        );

        // key. all relative partitions combine into one partition.
        PlaceSet combinedPartition = PlaceSet.combine(partitions);
        // value. all predicates relative to these partitions.
        Map<PlaceSet, List<Predicate<TokenSet>>> combinedPredicates = new HashMap<>();

        // for each partition to be combined, remove from the original map.
        // then combine multi-maps into one map.
        partitions.forEach(partition -> combinedPredicates.putAll(predicateItems.remove(partition)));
        predicateItems.put(combinedPartition, combinedPredicates);

        combinedPredicates.putIfAbsent(placeSet, new ArrayList<>()).add(predicate);
    }

    /**
     * test if a tokenSet satisfy all the predicates.
     *
     * @param tokenSet
     * @return
     */
    public boolean test(PlaceSet partition, TokenSet tokenSet) {
        if (predicateItems.get(partition) == null) {
            return true;
        }

        Map<PlaceSet, List<Predicate<TokenSet>>> predicateItem = predicateItems.get(partition);

        for (Entry<PlaceSet, List<Predicate<TokenSet>>> placeSetPredicates : predicateItem.entrySet()) {
            PlaceSet placeSet = placeSetPredicates.getKey();
            List<Predicate<TokenSet>> predicates = placeSetPredicates.getValue();
            TokenSet subTokenSet = tokenSet.getByPids(placeSet);
            for (Predicate<TokenSet> predicate : predicates) {
                if (!predicate.test(subTokenSet)) {
                    return false;
                }
            }
        }

        return true;
    }

    public Collection<PlaceSet> getPlacePartition() {
        return predicateItems.keySet();
    }
}
