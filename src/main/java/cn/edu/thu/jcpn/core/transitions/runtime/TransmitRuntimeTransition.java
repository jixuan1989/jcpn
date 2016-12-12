package cn.edu.thu.jcpn.core.transitions.runtime;

import cn.edu.thu.jcpn.common.Pair;
import cn.edu.thu.jcpn.core.places.runtime.IOwner;
import cn.edu.thu.jcpn.core.places.runtime.ITarget;
import cn.edu.thu.jcpn.core.places.runtime.RuntimeLocalPlace;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.condition.PlaceSet;
import cn.edu.thu.jcpn.core.transitions.condition.TokenSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class TransmitRuntimeTransition extends RuntimeTransition {

    private static Logger logger = LogManager.getLogger();

    private Set<ITarget> targets;
    private Map<ITarget, Map<PlaceSet, List<TokenSet>>> cache;

    public TransmitRuntimeTransition(Transition transition, IOwner owner, Set<ITarget> targets) {
        super(transition, owner);
        this.targets = targets;
        this.targets.remove(owner);
    }

    @Override
    public void checkNewlyTokens4Firing() {
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

        RuntimeLocalPlace place = (RuntimeLocalPlace) inPlaces.get(pids.get(position));
        List<IToken> tokens = place.getNewlyTokens();
        for (int i = 0; i < tokens.size(); ++i) {
            tokenSet.addToken(pids.get(position), tokens.get(i));
            findAndSave(availableTokens, placeSet, tokenSet, pids, position + 1);
            tokenSet.removeToken(pids.get(position));
        }
    }

    public boolean canFire() {
        for (Map.Entry<PlaceSet, List<TokenSet>> entry : cache.entrySet()) {
            if (entry.getValue().size() == 0) {
                return false;
            }
        }
        return true;
    }

    public IOutputTokenBinding firing() {
        // random get a tokenSet from each partition, and merge into one tokenSet.
        // return it and remove from cache including all tokens relative to tokenSet.
        // then remove tokens of this tokenSet from these origin places.
        TokenSet tokenSet = new TokenSet();
        cache.forEach((placeSet, tokenSets) -> tokenSet.addTokenSet(tokenSets.get(random.nextInt() % tokenSets.size())));
        return super.firing(tokenSet);
    }

    @Override
    public void removeTokenFromCache(TokenSet tokenSet) {
        for (Entry<PlaceSet, List<TokenSet>> cacheEntry : cache.entrySet()) {
            PlaceSet placeSet = cacheEntry.getKey();
            PlaceSet intersectedSet = PlaceSet.intersect(placeSet, tokenSet.keySet());
            if (intersectedSet.isEmpty()) {
                continue;
            }

            List<TokenSet> tokenSets = cacheEntry.getValue();
            for (TokenSet innerTokenSet : tokenSets) {
                //TODO
            }
        }

    }

    public Map<PlaceSet, List<TokenSet>> getAllPartitionedAvailableTokens() {
        return cache;
    }
}
