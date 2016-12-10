package cn.edu.thu.jcpn.core.place;

import java.util.HashMap;
import java.util.Map;

public abstract class Place {

    protected int id;
    protected String name;
    protected PlaceType type;

    protected PlaceStrategy strategy;

    /**
     * <ransition id, number of required tokens>
     */
    protected Map<Integer, Integer> outArcs;

    /**
     * bag: disordered
     * <br>
     * fifo:  if time is different, ordering them. If time are the same, disordering them.
     * <br>
     *
     * @author hxd
     */
    public enum PlaceStrategy {
        BAG, FIFO
    }

    public enum PlaceType {
        IndividualPlace, ConnectionPlace
    }

    public Place() {
        outArcs = new HashMap<>();
        strategy = PlaceStrategy.BAG;
    }

    public Place(int id) {
        this();
        this.id = id;
    }

    public Place(int id, String name) {
        this(id);
        this.name = name;
    }

    public PlaceStrategy getPlaceStrategy() {
        return strategy;
    }

    public void setPlaceStrategy(PlaceStrategy type) {
        if (this.strategy != type) {
            resetType(type);
        }
        this.strategy = type;
    }

    private void resetType(PlaceStrategy newType) {
        //TODO firstly, we only support BAG.
        //if it is a FIFO before resetting, we need to shuffle the queue.
//		this.initialTokens=copyOneCollection(this.initialTokens, (newType==PlaceType.BAG?new HashBag<>():new ArrayList<>()));
//		this.currentTokens=copyOneCollection(this.currentTokens, (newType==PlaceType.BAG?new HashBag<>():new ArrayList<>()));
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

    public Map<Integer, Integer> getOutArcs() {
        return outArcs;
    }

    public void setOutArcs(Map<Integer, Integer> outArcs) {
        this.outArcs = outArcs;
    }

    public Place addOutput(Integer transitionId, int number) {
        outArcs.put(transitionId, number);
        return this;
    }

    public PlaceType getType() {
        return type;
    }

    public void setType(PlaceType type) {
        this.type = type;
    }
}
