package cn.edu.thu.jcpn.core.runtime.token;

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
        return "UnitToken [time=" + time + "]";
    }

    public UnitToken() {
        super();
        time = GlobalClock.getInstance().getTime();
    }


    public static List<IToken> generateList(int number) {
        List<IToken> list = new ArrayList<>();
        IntStream.range(0, number).forEach(i -> list.add(new UnitToken()));
        return list;
    }
}
