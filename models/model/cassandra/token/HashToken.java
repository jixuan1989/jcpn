package model.cassandra.token;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.List;

/**
 * Created by leven on 2017/1/2.
 */
public class HashToken extends IToken {

    private int hashCode;
    private List<INode> nodes;

    public HashToken(int hashCode, List<INode> nodes) {
        super();
        super.setTimeCost(0);
        this.hashCode = hashCode;
        this.nodes = nodes;
    }

    public int getHashCode() {
        return hashCode;
    }

    public List<INode> getNodes() {
        return nodes;
    }
}
