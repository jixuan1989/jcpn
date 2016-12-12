package cn.edu.thu.jcpn.core.places;

import static cn.edu.thu.jcpn.core.places.Place.PlaceStrategy.*;

public abstract class Place {

    protected int id;
    protected String name;
    protected PlaceType type;

    protected PlaceStrategy strategy = BAG;

    /**
     * <br>BAG : random sort the tokens.
     * <br>FIFO : sort the tokens by their time, if exists tokens having same time, random sort this range tokens.
     */
    public enum PlaceStrategy {
        BAG, FIFO
    }

    /**
     * <br>LOCAL : a place is local if it point to a local transitions.
     * <br>COMMUNICATING: a place is communicating if it point to a transmit transitions.
     */
    public enum PlaceType {
        LOCAL, COMMUNICATING
    }

    public Place() {
        strategy = BAG;
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

    public void setPlaceStrategy(PlaceStrategy strategy) {
        if (this.strategy != strategy) {
            resetStrategy(strategy);
        }
        this.strategy = strategy;
    }

    private void resetStrategy(PlaceStrategy strategy) {
        //TODO firstly, we only support BAG.
        if (strategy == FIFO) {

        }
        else {

        }
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

    public PlaceType getType() {
        return type;
    }

    public void setType(PlaceType type) {
        this.type = type;
    }
}
