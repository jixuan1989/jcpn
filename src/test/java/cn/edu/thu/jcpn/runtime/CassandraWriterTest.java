package cn.edu.thu.jcpn.runtime;

import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.lookuptable.LookupTable;
import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.place.Place.PlaceType;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.StringNode;
import cn.edu.thu.jcpn.core.runtime.tokens.UnitToken;
import cn.edu.thu.jcpn.core.transition.Transition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by leven on 2016/12/25.
 */
public class CassandraWriterTest {

    private static Logger logger = LogManager.getLogger();

    private static int SERVER_NUMBER = 2;

    private CPN cpn;
    private List<INode> nodes;
    private RuntimeFoldingCPN instance;

    private GlobalClock globalClock = GlobalClock.getInstance();

    @Before
    public void initCassandraWriter() {

        cpn = new CPN();
        cpn.setVersion("1.0");

        nodes = IntStream.rangeClosed(1, SERVER_NUMBER).
                mapToObj(x -> new StringNode("server" + x)).collect(Collectors.toList());


        // global placeId and placeName.
        Place place1 = new Place(1, "unstarted servers", PlaceType.LOCAL);
        nodes.forEach(node -> place1.addInitToken(node, new UnitToken()));

        Place place2 = new Place(2, "alive servers", PlaceType.LOCAL);

        LookupTable lookupTable3 = new LookupTable(3, "lookup table");

        Place place4 = new Place(4, "write resources");
        Place place5 = new Place(5, "response resources");
        Place place6 = new Place(6, "network resources");
        Place place7 = new Place(7, "connection notifications");
        Place place8 = new Place(8, "received connections");
        Place place9 = new Place(9, "client requests");
        Place place10 = new Place(10, "requests for sending");
        Place place11 = new Place(11, "requests for writing");
        Place place12 = new Place(12, "callbacks");
        Place place13 = new Place(13, "received tasks");
        Place place14 = new Place(14, "requests for sync");
        Place place15 = new Place(15, "mutated task");
        Place place16 = new Place(16, "data store");

        Map<Integer, Place> placeMap = new HashMap<>();
        placeMap.put(1, place1);
        placeMap.put(2, place2);
        //placeMap.put(3, place3);
        placeMap.put(4, place4);
        placeMap.put(5, place5);
        placeMap.put(6, place6);
        placeMap.put(7, place7);
        placeMap.put(8, place8);
        placeMap.put(9, place9);
        placeMap.put(10, place10);
        placeMap.put(11, place11);
        placeMap.put(12, place12);
        placeMap.put(13, place13);
        placeMap.put(14, place14);
        placeMap.put(15, place15);
        placeMap.put(16, place16);


        // global transitionId and transitionName.
        Transition transition1 = new Transition(1, "start up");
        transition1.addInPlace(place1);
        //transition1.addOutPlace(place2).addOutPlace(place3).addOutPlace(place4).addOutPlace(place5).addOutPlace(place6).addOutPlace(place7);

        Transition transition2 = new Transition(2, "connection notify");
        transition2.addInPlace(place7);
        transition2.addOutPlace(place8);

        Transition transition3 = new Transition(3, "complete connection");
        transition3.addInPlace(place8);
        transition3.addOutPlace(place6);

        Transition transition4 = new Transition(4, "scan lookup table");
        //transition4.addInPlace(place3).addInPlace(place9);
        transition4.addOutPlace(place10).addOutPlace(place11).addOutPlace(place12);

        Transition transition5 = new Transition(5, "network transmit");
        transition5.addInPlace(place6).addInPlace(place10);
        transition5.addOutPlace(place6).addOutPlace(place13);

        Transition transition6 = new Transition(6, "put received task into the write queue");
        transition6.addInPlace(place13);
        transition6.addOutPlace(place11);

        Transition transition7 = new Transition(7, "put received task into the sync queue");
        transition7.addInPlace(place13);
        transition7.addOutPlace(place14);

        Transition transition8 = new Transition(8, "execute mutate");
        transition8.addInPlace(place4).addInPlace(place11);
        transition8.addOutPlace(place4).addOutPlace(place15).addOutPlace(place16);;

        Transition transition9 = new Transition(9, "generate response msg for sending back");
        transition9.addInPlace(place15);
        transition9.addOutPlace(place10);

        Transition transition10 = new Transition(10, "generate response msg for sync");
        transition10.addInPlace(place15);
        transition10.addOutPlace(place14);

        Transition transition11 = new Transition(11, "sync");
        transition11.addInPlace(place5).addInPlace(place12).addInPlace(place14);
        transition11.addOutPlace(place5).addOutPlace(place12);

        Transition transition12 = new Transition(12, "redundant sync");
        transition12.addInPlace(place5).addInPlace(place14);
        transition12.addOutPlace(place14);

        Map<Integer, Transition> transitionMap = new HashMap<>();
        transitionMap.put(1, transition1);
        transitionMap.put(2, transition2);
        transitionMap.put(3, transition3);
        transitionMap.put(4, transition4);
        transitionMap.put(5, transition5);
        transitionMap.put(6, transition6);
        transitionMap.put(7, transition7);
        transitionMap.put(8, transition8);
        transitionMap.put(9, transition9);
        transitionMap.put(10, transition10);
        transitionMap.put(11, transition11);
        transitionMap.put(12, transition12);


//
//        //p1中保存每个服务器的一个token,用unitToken表示
//        nodes.forEach(owner -> place1.addInitToken(new UnitToken(owner, LocalAsTarget.getInstance())));
//
//        //p3中保存消息
//        //p4中保存每个服务器和别的服务器的套接字token,也用unitColor表示
//        nodes.forEach(innerOwner -> nodes.stream().filter(innerTarget -> !innerTarget.equals(innerOwner)).
//                forEach(innerTarget -> {
//                    place3.addInitToken(new MessageToken(innerOwner, innerTarget, 0));
//                    place4.addInitToken(new UnitToken(innerOwner, innerTarget));
//                }));
//
//        //t1,t2写output函数
//        transition1.setOutputFunction(
//                inputToken -> {
//                    OutputToken outputToken = new OutputToken();
//
//                    MessageToken received = (MessageToken) inputToken.get(PID_2);
//                    MessageToken toSend = new MessageToken(received.getMessage() + 1);
//                    toSend.setOwner(received.getOwner());
//                    toSend.setTo(received.getTo());
//                    toSend.setTime(globalClock.getTime() + 1);
//                    outputToken.addToken(toSend.getTo(), PID_3, toSend);
//
//                    IToken thread = inputToken.get(PID_1);
//                    thread.setTime(globalClock.getTime() + 1);
//                    outputToken.addToken(LocalAsTarget.getInstance(), PID_1, thread);
//
//                    return outputToken;
//                }
//        );
//
//        transition2.setOutputFunction(
//                inputToken -> {
//                    OutputToken outputToken = new OutputToken();
//
//                    MessageToken toSend = (MessageToken) inputToken.get(PID_3);
//                    MessageToken received = new MessageToken(toSend.getMessage() + 1);
//                    received.setOwner(toSend.getOwner());
//                    received.setTo(toSend.getTo());
//                    received.setTime(globalClock.getTime() + 1);
//                    outputToken.addToken(received.getTo(), PID_2, received);
//
//                    IToken socket = inputToken.get(PID_4);
//                    socket.setTime(globalClock.getTime() + 1);
//                    outputToken.addToken(LocalAsTarget.getInstance(), PID_4, socket);
//
//                    return outputToken;
//                }
//        );

        cpn.setPlaces(placeMap);
        cpn.setTransitions(transitionMap);
        instance = new RuntimeFoldingCPN(cpn, nodes);
    }
}
