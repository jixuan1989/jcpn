package cn.edu.thu.jcpn.core.transitions;

import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.transitions.condition.Condition;
import cn.edu.thu.jcpn.core.transitions.condition.PlaceSet;
import cn.edu.thu.jcpn.core.transitions.condition.TokenSet;

import java.util.*;
import java.util.function.Predicate;

public abstract class Transition {

    protected int id;
    protected String name;
    protected TransitionType type;
    protected int priority = 500;

    protected Condition condition;

    /**
     * <place, number of required tokens>
     */
    protected Set<Place> inPlaces;

    /**
     * <place, number of required tokens>
     */
    protected Set<Place> outPlaces;

    protected Function<TokenSet, IOutputTokenBinding> outputFunction;

    public enum TransitionType {
        LOCAL, TRANSMIT
    }

    public Transition(int id, String name) {
        super();
        this.id = id;
        this.name = name;
        inPlaces = new HashSet<>();
        outPlaces = new HashSet<>();
        condition = new Condition();
    }

    public Set<Place> getInPlaces() {
        return inPlaces;
    }

    public void setInPlaces(Set<Place> inPlaces) {
        this.inPlaces = inPlaces;
    }

    public Transition addInput(Place place) {
        inPlaces.add(place);
        return this;
    }

    public Set<Place> getOutPlaces() {
        return outPlaces;
    }

    public void setOutPlaces(Set<Place> outPlaces) {
        this.outPlaces = outPlaces;
    }

    public Transition addOutput(Place place) {
        outPlaces.add(place);
        return this;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransitionType getType() {
        return type;
    }

    public void setType(TransitionType type) {
        this.type = type;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * @param outputFunction notice the time cost is relative time rather than absolute time
     */
    public void setOutputFunction(Function<TokenSet, IOutputTokenBinding> outputFunction) {
        this.outputFunction = outputFunction;
    }

    public Condition getCondition() {
        return condition;
    }

    public void addCondition(PlaceSet placeSet, Predicate<TokenSet> predicate) {
        condition.addPredicate(placeSet, predicate);
    }
}

/*
    public void complie() {
        condition.getPlacePartition().forEach(partition -> {
            List<TokenSet> availableTokens = new ArrayList<>();
            TokenSet tokenSet = new TokenSet();
            findAndSave(availableTokens, partition, tokenSet, partition.getPids(), 0);
            cache.put(partition, new ArrayList<>()).addAll(availableTokens);
        });
    }

    private void findAndSave(List<TokenSet> availableTokens, PlaceSet placeSet, TokenSet tokenSet, Integer[] pids, int position) {
        if (position == pids.length) {
            if (condition.test(placeSet, tokenSet)) {
                availableTokens.add(new TokenSet(tokenSet));
            }
            return;
        }

        RuntimeLocalPlace place = (RuntimeLocalPlace) inputPlaces.get(pids[position]);
        List<IToken> tokens = place.getNewlyTokens();
        for (int i = 0; i < tokens.size(); ++i) {
            tokenSet.addToken(pids[position], tokens.get(i));
            findAndSave(availableTokens, placeSet, tokenSet, pids, position + 1);
            tokenSet.removeToken(pids[position]);
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
        cache.forEach((placeSet, tokenSets) -> {
            selectedTokens.put(placeSet, random.nextInt() % tokenSets.size());
        });
        return this.fire(selectedTokens);
    }

    public TokenSet fire(Map<PlaceSet, Integer> selectedTokens) {
        // random get a tokenSet from each partition, and merge into one tokenSet.
        // return it and remove from cache including all tokens relative to tokenSet.
        // then remove tokens of this tokenSet from these origin places.
        List<TokenSet> randomTokens = new ArrayList<>();
        cache.forEach((placeSet, tokenSets) -> {
            TokenSet temp = tokenSets.get(selectedTokens.get(placeSet));
            randomTokens.add(temp);
            //TODO remove chosen tokens.
        });
        return TokenSet.combine(randomTokens);
    }

    public Map<PlaceSet, List<TokenSet>> getAllPartitionsAvailableTokens() {
        return cache;
    }
 */
