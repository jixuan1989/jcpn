package cn.edu.thu.jcpn.runtime;

import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.container.Place;
import cn.edu.thu.jcpn.core.container.Place.PlaceType;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.*;
import cn.edu.thu.jcpn.core.executor.transition.Transition;
import cn.edu.thu.jcpn.core.executor.transition.Transition.TransitionType;
import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;
import cn.edu.thu.jcpn.elements.token.MessageToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by leven on 2016/11/21.
 */
public class SimpleDistributedDatabaseTest {

    private static Logger logger = LogManager.getLogger();

    private static int SERVER_NUMBER = 2;

    private CPN cpn;
    private List<INode> nodes;
    private RuntimeFoldingCPN instance;

    @Before
    public void initSimpleDistributedDatabase() {

        cpn = new CPN("simpleDD");


        Place place1 = new Place("local", PlaceType.LOCAL);
        Place place2 = new Place("received", PlaceType.LOCAL);
        Place place3 = new Place("toSend", PlaceType.COMMUNICATING);
        Place place4 = new Place("socket", PlaceType.COMMUNICATING);

        Transition transition1 = new Transition("execute", TransitionType.LOCAL);
        Transition transition2 = new Transition("transmit", TransitionType.TRANSMIT);

        transition1.addInContainer(place1).addInContainer(place2);
        transition1.addOutContainer(place1).addOutContainer(place3);

        transition2.addInContainer(place3).addInContainer(place4);
        transition2.addOutContainer(place4).addOutContainer(place2);

        nodes = IntStream.rangeClosed(1, SERVER_NUMBER).
                mapToObj(x -> new StringNode("server" + x)).collect(Collectors.toList());

        //p1中保存每个服务器的一个token,用unitToken表示
        nodes.forEach(node -> place1.addInitToken(node, new UnitToken()));

        place3.addInitToken(null, nodes.get(0), nodes.get(1), new MessageToken(0));
        //p3中保存消息
        //p4中保存每个服务器和别的服务器的套接字token,也用unitColor表示
        nodes.forEach(owner -> nodes.stream().filter(to -> !to.equals(owner)).
                forEach(to -> {
                    place4.addInitToken(null, owner, to, new UnitToken());
                }));

        //t1,t2写output函数
        transition1.setTransferFunction(
                inputToken -> {
                    OutputToken outputToken = new OutputToken();
                    MessageToken received = (MessageToken) inputToken.get(place2.getId());
                    MessageToken toSend = new MessageToken(received.getMessage() + 1);
                    toSend.setTo(received.getFrom());
                    toSend.setTimeCost(1);
                    outputToken.addToken(received.getOwner(), place3.getId(), toSend);

                    IToken thread = inputToken.get(place1.getId());
                    thread.setTimeCost(1);
                    outputToken.addToken(thread.getOwner(), place1.getId(), thread);

                    return outputToken;
                }
        );

        transition2.setTransferFunction(
                inputToken -> {
                    OutputToken outputToken = new OutputToken();
                    MessageToken toSend = (MessageToken) inputToken.get(place3.getId());
                    MessageToken received = new MessageToken(toSend.getMessage() + 1);
                    received.setFrom(toSend.getOwner());
                    received.setTimeCost(1);
                    outputToken.addToken(toSend.getTo(), place2.getId(), received);

                    IToken socket = inputToken.get(place4.getId());
                    socket.setTimeCost(1);
                    outputToken.addToken(socket.getOwner(), place4.getId(), socket);

                    return outputToken;
                }
        );

        cpn.addContainers(place1, place2, place3, place4);
        cpn.addTransitions(transition1, transition2);
        instance = new RuntimeFoldingCPN();
        instance.setVersion("1.0");

        instance.addCpn(cpn, nodes);

        ITransitionMonitor transitionMonitor = (time, owner, transitionId, transitionName, inputToken, outputToken) -> System.out.println(owner + "'s " + transitionName + " is fired");
        instance.addMonitor(transition1.getId(), transitionMonitor);
        instance.addMonitor(transition2.getId(), transitionMonitor);
    }

    @Test
    public void test0() throws InterruptedException {
        int count = 0;
        while (instance.hasNextTime()) {
            instance.nextRound();
            if (count++ == 66) break;
        }
    }
}
