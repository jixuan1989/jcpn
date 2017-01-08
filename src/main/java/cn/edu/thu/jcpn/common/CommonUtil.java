package cn.edu.thu.jcpn.common;

import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.StringNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by leven on 2017/1/8.
 */
public class CommonUtil {

    public static List<INode> generateStringNodes(String name, int number) {
        return IntStream.rangeClosed(1, number).
                mapToObj(seqNo -> new StringNode(name + seqNo)).collect(Collectors.toList());
    }
}
