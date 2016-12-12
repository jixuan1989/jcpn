package cn.edu.thu.jcpn.core.places.runtime;

import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.places.Place.*;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class RuntimePlace {

    protected int id;
    protected String name;
    protected PlaceType type;
    protected PlaceStrategy placeStrategy;

    protected GlobalClock globalClock;
    protected static Random random = new Random();

    public RuntimePlace(Place place) {
        id = place.getId();
        name = place.getName();
        type = place.getType();
        placeStrategy = place.getPlaceStrategy();

        globalClock = GlobalClock.getInstance();
    }

    public Integer getId() {
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

    public PlaceStrategy getPlaceStrategy() {
        return placeStrategy;
    }

    public void setPlaceStrategy(PlaceStrategy placeStrategy) {
        this.placeStrategy = placeStrategy;
    }

    public PlaceType getType() {
        return type;
    }

    public void setType(PlaceType type) {
        this.type = type;
    }

    /**
     * move all the tokens from the newly queue to the test queue.
     */
    public abstract void markTokensAsTested();

    /**
     * check whether newly tokens is empty.
     * This method will check all the future tokens, and reassign tokens into newly or future queues, by comparing with global clock.
     * @return true if after reassigning, newly tokens is not empty;
     *         false, otherwise.
     */
    public abstract boolean hasNewlyTokens();

    /**
     * add a new token into existing tokens, order by their time.
     * If there exists multi tokens whose time are equal to the newly token,
     * random insert the newly token into them.
     *
     * @param tokens existing tokens
     * @param token new token that to be added
     */
    protected void addTokenByTimeOrder(List<IToken> tokens, IToken token) {
        int start = 0;
        int end = tokens.size() - 1;
        while (start + 1 < end) {
            int mid = start + (end - start) / 2;
            if (tokens.get(mid).getTime() < token.getTime()) {
                start = mid;
            }
            else { // tokens[mid].time >= token.time.
                end = mid;
            }
        }

        if (tokens.get(start).getTime() > token.getTime()) {
            tokens.add(start, token); // add(0, token);
            return;
        }
        else if (tokens.get(end).getTime() < token.getTime()) {
            tokens.add(end + 1, token); // add(tokens.length, token);
            return;
        }
        else if (tokens.get(start).getTime() < token.getTime() &&
                tokens.get(end).getTime() > token.getTime()) {
            tokens.add(end, token);
            return;
        }// otherwise, there exists some tokens that tokens.time equals to token.time.

        int insertStart = tokens.get(start).getTime() == token.getTime() ? start : end;

        start = insertStart;
        end = tokens.size() - 1;
        while (start + 1 < end) {
            int mid = start + (end - start) / 2;
            if (tokens.get(mid).getTime() <= token.getTime()) {
                start = mid;
            }
            else { // tokens[mid].time > token.time.
                end = mid;
            }
        }

        int insertEnd = (tokens.get(end).getTime() == token.getTime()) ? end : start;

        tokens.add(random.nextInt(insertEnd - insertStart + 1) + insertStart, token);
    }

    public abstract void logStatus();
}
