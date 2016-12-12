package cn.edu.thu.jcpn.core.places;

import cn.edu.thu.jcpn.core.places.runtime.IOwner;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalPlace extends Place {

    private static Logger logger = LogManager.getLogger();

    private Map<IOwner, List<IToken>> initialTokens;

    public LocalPlace(int id, String name) {
        super(id, name);
        initialTokens = new HashMap<>();
        this.setType(PlaceType.LOCAL);
    }

    public Map<IOwner, List<IToken>> getInitialTokens() {
        return initialTokens;
    }

    public void setInitialTokens(Map<IOwner, List<IToken>> initialTokens) {
        this.initialTokens = initialTokens;
    }

    public LocalPlace addInitToken(IOwner owner, IToken token) {
        initialTokens.computeIfAbsent(owner, obj -> new ArrayList<>()).add(token);
        return this;
    }
}
