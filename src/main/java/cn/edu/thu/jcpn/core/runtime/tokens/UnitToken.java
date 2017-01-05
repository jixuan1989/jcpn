package cn.edu.thu.jcpn.core.runtime.tokens;

import cn.edu.thu.jcpn.core.runtime.GlobalClock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * UNIT type colset, used to represent a meaningless or unconcern message.
 *
 * @author hxd
 */
public class UnitToken extends IToken {

    @Override
    public String toString() {
        return "UnitToken {from=" + from + ",owner=" + owner + ",to=" + to + ",time=" + time + "}";
    }

    public UnitToken() {
        super();
        time = GlobalClock.getInstance().getTime();
    }
}
