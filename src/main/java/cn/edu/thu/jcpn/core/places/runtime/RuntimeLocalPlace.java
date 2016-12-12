package cn.edu.thu.jcpn.core.places.runtime;

import cn.edu.thu.jcpn.core.places.LocalPlace;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.places.Place.PlaceStrategy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RuntimeLocalPlace extends RuntimePlace {

    private static Logger logger = LogManager.getLogger();

    protected IOwner owner;
    protected List<IToken> newlyTokens = new ArrayList<>();
    protected List<IToken> testedTokens = new ArrayList<>();
    protected List<IToken> futureTokens = new ArrayList<>();

    public RuntimeLocalPlace(LocalPlace place, IOwner owner) {
        super(place);
        this.owner = owner;
        newlyTokens = new ArrayList<>();
        testedTokens = new ArrayList<>();
        futureTokens = new ArrayList<>();
    }

    public RuntimeLocalPlace(LocalPlace place, IOwner owner, List<IToken> tokens) {
        this(place, owner);
        addTokens(owner, tokens);
    }

    public IOwner getOwner() {
        return owner;
    }

    public void setOwner(IOwner owner) {
        this.owner = owner;
    }

    public List<IToken> getNewlyTokens() {
        return newlyTokens;
    }

    public void setNewlyTokens(List<IToken> newlyTokens) {
        this.newlyTokens = newlyTokens;
    }

    public List<IToken> getTestedTokens() {
        return testedTokens;
    }

    public void setTestedTokens(List<IToken> testedTokens) {
        this.testedTokens = testedTokens;
    }

    public List<IToken> getFutureTokens() {
        return futureTokens;
    }

    public void setFutureTokens(List<IToken> futureTokens) {
        this.futureTokens = futureTokens;
    }

    public void addTokens(IOwner owner, List<IToken> tokens) {
        if (!owner.equals(this.owner)) {
            logger.error("individual place instance requires the same owner for addTokens");
        }
        
        if (this.getPlaceStrategy().equals(PlaceStrategy.BAG)) {
            assignTokensIntoDifferentQueues(tokens,
                    token -> futureTokens.add(random.nextInt(futureTokens.size() + 1), token),
                    token -> newlyTokens.add(random.nextInt(newlyTokens.size() + 1), token)
            );
        } else {
            //FIFO is more complicate. If two tokens have the same time, they are disordered. otherwise, they are ordered.
            assignTokensIntoDifferentQueues(tokens,
                    token -> addTokenByTimeOrder(futureTokens, token),
                    token -> addTokenByTimeOrder(newlyTokens, token)
            );
        }
    }

    /**
     * assign the given tokens into the future token queue or the newly token queue.
     *
     * @param tokens
     * @param futuerConsumer
     * @param newlyConsumer
     */
    private void assignTokensIntoDifferentQueues(List<IToken> tokens, Consumer<IToken> futuerConsumer, Consumer<IToken> newlyConsumer) {
        tokens.stream().forEach(color -> {
            if (color.getTime() > globalClock.getTime()) {
                futuerConsumer.accept(color);
            } else {
                newlyConsumer.accept(color);
            }
        });
    }

    @Override
    public void markTokensAsTested() {
        testedTokens.addAll(newlyTokens);
        newlyTokens.clear();
    }

    public void removeTokenFromTest(IToken token) {
        testedTokens.remove(token);
    }

    @Override
    public boolean hasNewlyTokens() {
        List<IToken> timeUp = futureTokens.stream().filter(token -> token.getTime() <= globalClock.getTime()).collect(Collectors.toList());
        futureTokens.removeAll(timeUp);
        newlyTokens.addAll(timeUp);
        return !this.newlyTokens.isEmpty();
    }

    @Override
    public void logStatus() {
        StringBuilder sb = new StringBuilder();
        if (newlyTokens.size() > 0) {
            newlyTokens.forEach(newlyToken -> sb.append("\t" + newlyToken.toString() + " newly "));
        }
        if (testedTokens.size() > 0) {
            sb.append("\n");
            testedTokens.forEach(testedToken -> sb.append("\t" + testedToken.toString() + " tested "));
        }
        if (futureTokens.size() > 0) {
            sb.append("\n");
            futureTokens.forEach(futureToken -> sb.append("\t" + futureToken.toString() + " future "));
        }
        System.out.println(String.format("place : %s", getName()));
        System.out.println(sb.toString());
    }
}
