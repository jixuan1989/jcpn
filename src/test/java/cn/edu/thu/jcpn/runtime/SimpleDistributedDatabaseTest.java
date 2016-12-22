package cn.edu.thu.jcpn.runtime;

import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.runtime.AutoSimulator;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.*;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.condition.OutputToken;
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
    private List<IOwner> owners;
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
//        PlacePartition partition1 = new PlacePartition();
//        partition1.add(PID_1);
//        partition1.add(PID_2);
//        transition1.addCondition(partition1, inputToken -> {
//            IToken thread = inputToken.get(PID_1);
//            IToken message = inputToken.get(PID_2);
//            return thread.getOwner().equals(message.getOwner());
//        });
        transition1.addOutPlace(place1).addOutPlace(place3);

        transition2.addInPlace(place3).addInPlace(place4);
//        PlacePartition partition2 = new PlacePartition();
//        partition2.add(PID_3);
//        partition2.add(PID_4);
//        transition1.addCondition(partition2, inputToken -> {
//            IToken message = inputToken.get(PID_3);
//            IToken socket = inputToken.get(PID_4);
//            return message.getOwner().equals(socket.getOwner()) && message.getTarget().equals(socket.getOwner());
//        });
        transition2.addOutPlace(place4).addOutPlace(place2);

        owners = IntStream.rangeClosed(1, SERVER_NUMBER).
                mapToObj(x -> new StringOwner("server" + x)).collect(Collectors.toList());

        //p1中保存每个服务器的一个token,用unitToken表示
        owners.forEach(owner -> place1.addInitToken(new UnitToken(owner, LocalAsTarget.getInstance())));

        IToken token = new MessageToken(0);
        token.setOwner(owners.get(0));
        token.setTarget(owners.get(1));
        place3.addInitToken(token);

        //p3中保存消息
        //p4中保存每个服务器和别的服务器的套接字token,也用unitColor表示
        owners.forEach(innerOwner -> owners.stream().filter(innerTarget -> !innerTarget.equals(innerOwner)).
                forEach(innerTarget -> place4.addInitToken(new UnitToken(innerOwner, innerTarget))));

        //t1,t2写output函数
        transition1.setOutputFunction(
                inputToken -> {
                    OutputToken outputToken = new OutputToken();

                    MessageToken received = (MessageToken) inputToken.get(PID_2);
                    MessageToken toSend = new MessageToken(received.getMessage() + 1);
                    toSend.setOwner((IOwner) received.getTarget());
                    toSend.setTarget(received.getOwner());
                    toSend.setTime(globalClock.getTime() + 1);
                    outputToken.addToken(toSend.getTarget(), PID_3, toSend);

                    IToken thread = inputToken.get(PID_1);
                    thread.setTime(globalClock.getTime() + 1);
                    outputToken.addToken(LocalAsTarget.getInstance(), PID_1, thread);

                    return outputToken;
                }
        );

        transition2.setOutputFunction(
                inputToken -> {
                    OutputToken outputToken = new OutputToken();

                    MessageToken toSend = (MessageToken) inputToken.get(PID_3);
                    MessageToken received = new MessageToken(toSend.getMessage() + 1);
                    received.setOwner((IOwner) toSend.getTarget());
                    received.setTarget(toSend.getOwner());
                    received.setTime(globalClock.getTime() + 1);
                    outputToken.addToken(received.getTarget(), PID_2, received);

                    IToken socket = inputToken.get(PID_4);
                    socket.setTime(globalClock.getTime() + 1);
                    outputToken.addToken(LocalAsTarget.getInstance(), PID_4, socket);

                    return outputToken;
                }
        );

        cpn.setPlaces(placeMap);
        cpn.setTransitions(transitionMap);
        instance = new RuntimeFoldingCPN(cpn, owners);
    }

    @Test
    public void test0() throws InterruptedException {
        AutoSimulator simulator = new AutoSimulator(instance);
        simulator.compile();

        while (simulator.hasNextTime()) {
            simulator.nextRound();
        }
    }
}
