package cn.edu.thu.jcpn.core.transitions;

import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.transitions.condition.Condition;
import cn.edu.thu.jcpn.core.transitions.condition.InputToken;
import cn.edu.thu.jcpn.core.transitions.condition.OutputToken;
import cn.edu.thu.jcpn.core.transitions.condition.PlacePartition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Transition {

    protected int id;
    protected String name;
    protected TransitionType type;
    protected int priority = 500;

    protected Condition condition;

    protected Set<Place> inPlaces;
    protected Set<Place> outPlaces;

    protected Function<InputToken, OutputToken> outputFunction;

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
    public void setOutputFunction(Function<InputToken, OutputToken> outputFunction) {
        this.outputFunction = outputFunction;
    }

    public Function<InputToken, OutputToken> getOutputFunction() {
        return outputFunction;
    }

    public Condition getCondition() {
        return condition;
    }

    public void addCondition(PlacePartition placeSet, Predicate<InputToken> predicate) {
        condition.addPredicate(placeSet, predicate);
    }
}

/*
    public void complie() {
        condition.getPlacePartition().forEach(partition -> {
            List<InputToken> availableTokens = new ArrayList<>();
            InputToken tokenSet = new InputToken();
            findAndSave(availableTokens, partition, tokenSet, partition.getPids(), 0);
            cache.put(partition, new ArrayList<>()).addAll(availableTokens);
        });
    }

    private void findAndSave(List<InputToken> availableTokens, PlacePartition placeSet, InputToken tokenSet, Integer[] pids, int position) {
        if (position == pids.length) {
            if (condition.test(placeSet, tokenSet)) {
                availableTokens.add(new InputToken(tokenSet));
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
        for (Entry<PlacePartition, List<InputToken>> entry : cache.entrySet()) {
            if (entry.getValue().size() == 0) {
                return false;
            }
        }
        return true;
    }

    public InputToken fire() {
        Map<PlacePartition, Integer> selectedTokens = new HashMap<>();
        cache.forEach((placeSet, tokenSets) -> {
            selectedTokens.put(placeSet, random.nextInt() % tokenSets.size());
        });
        return this.fire(selectedTokens);
    }

    public InputToken fire(Map<PlacePartition, Integer> selectedTokens) {
        // random get a tokenSet from each partition, and merge into one tokenSet.
        // return it and remove from cache including all tokens relative to tokenSet.
        // then remove tokens of this tokenSet from these origin places.
        List<InputToken> randomTokens = new ArrayList<>();
        cache.forEach((placeSet, tokenSets) -> {
            InputToken temp = tokenSets.get(selectedTokens.get(placeSet));
            randomTokens.add(temp);
            //TODO remove chosen tokens.
        });
        return InputToken.combine(randomTokens);
    }

    public Map<PlacePartition, List<InputToken>> getAllPartitionsAvailableTokens() {
        return cache;
    }
 */
