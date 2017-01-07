package cn.edu.thu.jcpn.runtime;

import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.container.place.Place;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.*;
import cn.edu.thu.jcpn.core.transition.Transition;
import cn.edu.thu.jcpn.core.transition.condition.OutputToken;
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

    private GlobalClock globalClock = GlobalClock.getInstance();

    @Before
    public void initSimpleDistributedDatabase() {

        cpn = new CPN();
        cpn.setVersion("1.0");

        Map<Integer, Place> placeMap = new HashMap<>();
        // global placeId and placeName.
        Place place1 = new Place(1, "local");
        placeMap.put(1, place1);
        Place place2 = new Place(2, "received");
        placeMap.put(2, place2);
        Place place3 = new Place(3, "toSend");
        placeMap.put(3, place3);
        Place place4 = new Place(4, "socket");
        placeMap.put(4, place4);

        int PID_1 = 1;
        int PID_2 = 2;
        int PID_3 = 3;
        int PID_4 = 4;

        Map<Integer, Transition> transitionMap = new HashMap<>();
        // global transitionId and transitionName.
        Transition transition1 = new Transition(1, "execute");
        transitionMap.put(1, transition1);
        Transition transition2 = new Transition(2, "transmit");
        transitionMap.put(2, transition2);

        transition1.addInPlace(place1).addInPlace(place2);
        transition1.addOutPlace(place1).addOutPlace(place3);

        transition2.addInPlace(place3).addInPlace(place4);
        transition2.addOutPlace(place4).addOutPlace(place2);

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
                    MessageToken received = (MessageToken) inputToken.get(PID_2);
                    MessageToken toSend = new MessageToken(received.getMessage() + 1);
                    toSend.setTo(received.getFrom());
                    toSend.setTimeCost(1);
                    outputToken.addToken(received.getOwner(), PID_3, toSend);

                    IToken thread = inputToken.get(PID_1);
                    thread.setTimeCost(1);
                    outputToken.addToken(thread.getOwner(), PID_1, thread);

                    return outputToken;
                }
        );

        transition2.setTransferFunction(
                inputToken -> {
                    OutputToken outputToken = new OutputToken();
                    MessageToken toSend = (MessageToken) inputToken.get(PID_3);
                    MessageToken received = new MessageToken(toSend.getMessage() + 1);
                    received.setFrom(toSend.getOwner());
                    received.setTimeCost(1);
                    outputToken.addToken(toSend.getTo(), PID_2, received);

                    IToken socket = inputToken.get(PID_4);
                    socket.setTimeCost(1);
                    outputToken.addToken(socket.getOwner(), PID_4, socket);

                    return outputToken;
                }
        );

        cpn.setPlaces(placeMap);
        cpn.setTransitions(transitionMap);
        instance = new RuntimeFoldingCPN();
        instance.addCpn(cpn, nodes);

        ITransitionMonitor transitionMonitor = (owner, transitionId, transitionName, inputToken, outputToken) -> System.out.println( owner + "'s " + transitionName + " is fired");
        instance.addMonitor(transition1.getId(),transitionMonitor);
        instance.addMonitor(transition2.getId(),transitionMonitor);
    }

    @Test
    public void test0() throws InterruptedException {
        int count = 0;
        while (instance.hasNextTime()) {
            instance.nextRound();

//            if (count == 5) {
//                IOwner owner = nodes.get(0);
//                INode to = nodes.get(1);
//                IToken token = new MessageToken(owner, to, 1000);
//                List<IToken> tokens = new ArrayList<>();
//                tokens.add(token);
//                RuntimeIndividualCPN instanceIndividualCPN = instance.getIndividualCPN(owner, null);
//                instanceIndividualCPN.addNewlyTokens(3, tokens);
//            }

            if (count++ == 66) break;
        }
    }
}
