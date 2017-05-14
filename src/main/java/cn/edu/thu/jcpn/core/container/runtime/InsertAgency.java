package cn.edu.thu.jcpn.core.container.runtime;

import cn.edu.thu.jcpn.core.runtime.tokens.IToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by leven on 2017/2/17.
 */
public class InsertAgency {

    private IRuntimeContainer container;

    private List<IToken> queue;

    public InsertAgency(IRuntimeContainer container) {
        this.container = container;
        this.queue = new ArrayList<>();
    }

    public void accept(List<IToken> tokens) {
        queue.addAll(tokens);
    }

    public void shift() {
        container.addTokens(queue);
        queue.clear();
    }
}
