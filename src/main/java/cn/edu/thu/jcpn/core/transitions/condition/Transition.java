package cn.edu.thu.jcpn.core.transitions.condition;

import cn.edu.thu.jcpn.core.places.runtime.RuntimeLocalPlace;
import cn.edu.thu.jcpn.core.places.runtime.RuntimePlace;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * Created by leven on 2016/12/8.
 */
public class Transition {

    private Condition condition;
    private Map<PlaceSet, List<TokenSet>> cache;
    private Map<Integer, RuntimePlace> inputPlaceSet;
    private boolean complie;

    public static Random random = new Random();

    public Transition() {
        condition = new Condition();
        cache = new HashMap<>();
        inputPlaceSet = new HashMap<>();
        complie = false;
    }

    public void addCondition(PlaceSet placeSet, Predicate<TokenSet> predicate) {
        condition.addPredicate(placeSet, predicate);
    }

    public void complie() {
        condition.getPlacePartition().forEach(partition -> {
            List<TokenSet> availableTokens = new ArrayList<>();
            TokenSet tokenSet = new TokenSet();
            findAndSave(availableTokens, partition, tokenSet, partition.getPids(), 0);
            cache.put(partition, new ArrayList<>()).addAll(availableTokens);
        });
    }

    private void findAndSave(List<TokenSet> availableTokens, PlaceSet placeSet, TokenSet tokenSet, List<Integer> pids, int position) {
        if (position == pids.size()) {
            if (condition.test(placeSet, tokenSet)) {
                availableTokens.add(new TokenSet(tokenSet));
            }
            return;
        }

        RuntimeLocalPlace place = (RuntimeLocalPlace) inputPlaceSet.get(pids.get(position));
        List<IToken> tokens = place.getNewlyTokens();
        for (int i = 0; i < tokens.size(); ++i) {
            tokenSet.addToken(pids.get(position), tokens.get(i));
            findAndSave(availableTokens, placeSet, tokenSet, pids, position + 1);
            tokenSet.removeToken(pids.get(position));
        }
    }

    public boolean canFire() {
        for (Entry<PlaceSet, List<TokenSet>> entry : cache.entrySet()) {
            if (entry.getValue().size() == 0) {
                return false;
            }
        }
        return true;
    }

    public TokenSet fire() {
        Map<PlaceSet, Integer> selectedTokens = new HashMap<>();
        cache.forEach((partition, tokenSets) -> selectedTokens.put(partition, random.nextInt() % tokenSets.size()));
        return this.fire(selectedTokens);
    }

    public TokenSet fire(Map<PlaceSet, Integer> selectedTokens) {
        // random get a tokenSet from each partition, and merge into one tokenSet.
        // return it and remove from cache including all tokens relative to tokenSet.
        // then remove tokens of this tokenSet from these origin places.
        List<TokenSet> randomTokens = new ArrayList<>();
        cache.forEach((partition, tokenSets) -> {
            TokenSet temp = tokenSets.get(selectedTokens.get(partition));
            randomTokens.add(temp);
            //TODO remove chosen tokens.
        });
        return TokenSet.combine(randomTokens);
    }

    public Map<PlaceSet, List<TokenSet>> getAllPartitionsAvailableTokens() {
        return cache;
    }
}
