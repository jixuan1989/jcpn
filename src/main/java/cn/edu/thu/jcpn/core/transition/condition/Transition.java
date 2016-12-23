//package cn.edu.thu.jcpn.core.transitions.condition;
//
//import cn.edu.thu.jcpn.core.place.runtime.RuntimeLocalPlace;
//import cn.edu.thu.jcpn.core.place.runtime.RuntimePlace;
//import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
//
//import java.util.*;
//import java.util.Map.Entry;
//import java.util.function.Predicate;
//
///**
// * Created by leven on 2016/12/8.
// */
//public class Transition {
//
//    private Condition condition;
//    private Map<PlacePartition, List<InputToken>> cache;
//    private Map<Integer, RuntimePlace> inputPlaceSet;
//    private boolean complie;
//
//    public static Random random = new Random();
//
//    public Transition() {
//        condition = new Condition();
//        cache = new HashMap<>();
//        inputPlaceSet = new HashMap<>();
//        complie = false;
//    }
//
//    public void addCondition(PlacePartition placeSet, Predicate<InputToken> predicate) {
//        condition.addPredicate(placeSet, predicate);
//    }
//
//    public void complie() {
//        condition.getPlacePartition().forEach(partition -> {
//            List<InputToken> availableTokens = new ArrayList<>();
//            InputToken tokenSet = new InputToken();
//            findAndSave(availableTokens, partition, tokenSet, partition.getPids(), 0);
//            cache.put(partition, new ArrayList<>()).addAll(availableTokens);
//        });
//    }
//
//    private void findAndSave(List<InputToken> availableTokens, PlacePartition placeSet, InputToken tokenSet, List<Integer> pids, int position) {
//        if (position == pids.size()) {
//            if (condition.test(placeSet, tokenSet)) {
//                availableTokens.add(new InputToken(tokenSet));
//            }
//            return;
//        }
//
//        RuntimeLocalPlace place = (RuntimeLocalPlace) inputPlaceSet.get(pids.get(position));
//        List<IToken> tokens = place.getNewlyTokens();
//        for (int i = 0; i < tokens.size(); ++i) {
//            tokenSet.addToken(pids.get(position), tokens.get(i));
//            findAndSave(availableTokens, placeSet, tokenSet, pids, position + 1);
//            tokenSet.removeToken(pids.get(position));
//        }
//    }
//
//    public boolean hasEnableTransitions() {
//        for (Entry<PlacePartition, List<InputToken>> entry : cache.entrySet()) {
//            if (entry.getValue().size() == 0) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    public InputToken fire() {
//        Map<PlacePartition, Integer> selectedTokens = new HashMap<>();
//        cache.forEach((partition, tokenSets) -> selectedTokens.put(partition, random.nextInt() % tokenSets.size()));
//        return this.fire(selectedTokens);
//    }
//
//    public InputToken fire(Map<PlacePartition, Integer> selectedTokens) {
//        // random get a tokenSet from each partition, and merge into one tokenSet.
//        // return it and remove from cache including all tokens relative to tokenSet.
//        // then remove tokens of this tokenSet from these origin place.
//        List<InputToken> randomTokens = new ArrayList<>();
//        cache.forEach((partition, tokenSets) -> {
//            InputToken temp = tokenSets.get(selectedTokens.get(partition));
//            randomTokens.add(temp);
//            //TODO remove chosen tokens.
//        });
//        return InputToken.combine(randomTokens);
//    }
//
//    public Map<PlacePartition, List<InputToken>> getAllPartitionsAvailableTokens() {
//        return cache;
//    }
//}
