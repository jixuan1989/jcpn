package cn.edu.thu.jcpn.core.places;

import cn.edu.thu.jcpn.core.places.runtime.IOwner;
import cn.edu.thu.jcpn.core.places.runtime.ITarget;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunicatingPlace extends Place {

    private static Logger logger = LogManager.getLogger();

    private Map<IOwner, Map<ITarget, List<IToken>>> initialTokens;

    public CommunicatingPlace(int id, String name) {
        super(id, name);
        initialTokens = new HashMap<>();
        this.setType(PlaceType.COMMUNICATING);
    }

    public Map<IOwner, Map<ITarget, List<IToken>>> getInitialTokens() {
        return initialTokens;
    }

    public void setInitialTokens(Map<IOwner, Map<ITarget, List<IToken>>> initialTokens) {
        this.initialTokens = initialTokens;
    }

    public CommunicatingPlace addInitToken(IOwner owner, ITarget target, IToken token) {
        initialTokens.computeIfAbsent(owner, obj -> new HashMap<>())
                .computeIfAbsent(target, obj -> new ArrayList<>()).add(token);
        return this;
    }
}
