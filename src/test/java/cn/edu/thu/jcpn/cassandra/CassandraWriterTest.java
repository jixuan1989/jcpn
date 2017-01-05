package cn.edu.thu.jcpn.cassandra;

import cn.edu.thu.jcpn.cassandra.token.*;
import cn.edu.thu.jcpn.cassandra.token.ResponseToken.ResponseType;
import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.place.Place;
import cn.edu.thu.jcpn.core.place.Place.PlaceType;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.runtime.tokens.StringNode;
import cn.edu.thu.jcpn.core.runtime.tokens.UnitToken;
import cn.edu.thu.jcpn.core.transition.Transition;
import cn.edu.thu.jcpn.core.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.transition.condition.OutputToken;
import cn.edu.thu.jcpn.core.transition.condition.PlacePartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by leven on 2016/12/25.
 */
public class CassandraWriterTest {

    private static Logger logger = LogManager.getLogger();

    private static int SERVER_NUMBER = 5;
    private static int REPLICA = 3;

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
        Place place1 = new Place(1, "request", PlaceType.LOCAL);
        nodes.forEach(node -> place1.addInitToken(node, new RequestToken("name", node.getName(), 2)));

        Place place2 = new Place(2, "lookup table", PlaceType.CONSUMELESS);
        List<INode> cooperateNodes = new ArrayList<>();
        List<IToken> hashTable = new ArrayList<>();
        for (int i = 0; i < SERVER_NUMBER; ++i) {
            cooperateNodes.clear();
            for (int j = 0; j < REPLICA; ++j) {
                cooperateNodes.add(nodes.get((j + i) % SERVER_NUMBER));
            }
            hashTable.add(new HashToken(i, new ArrayList<>(cooperateNodes)));
        }
        nodes.forEach(node -> place2.addInitTokens(node, hashTable));

        Transition transition1 = new Transition(1, "schedule");
        transition1.addInPlace(place1).addInPlace(place2);
        PlacePartition partition1 = new PlacePartition();
        partition1.add(place1.getId());
        partition1.add(place2.getId());
        transition1.addCondition(partition1, inputToken -> {
            RequestToken request = (RequestToken) inputToken.get(place1.getId());
            HashToken hashToken = (HashToken) inputToken.get(place2.getId());
            int hashCode = request.getKey().hashCode() % SERVER_NUMBER;
            return hashCode == hashToken.getHashCode();
        });

        Place place3 = new Place(3, "sending queue", PlaceType.COMMUNICATING);
        Place place4 = new Place(4, "callback", PlaceType.LOCAL);

        Place place5 = new Place(5, "network resources", PlaceType.COMMUNICATING);
        nodes.forEach(owner -> nodes.stream().filter(to -> !to.equals(owner)).forEach(to ->
                place5.addInitToken(null, owner, to, new UnitToken())
        ));

        Place place6 = new Place(6, "received message", PlaceType.LOCAL);
        Place place7 = new Place(7, "writing queue", PlaceType.LOCAL);

        transition1.addOutPlace(place3).addOutPlace(place4).addOutPlace(place7);
        transition1.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            RequestToken request = (RequestToken) inputToken.get(place1.getId());
            INode cooperatorNode = request.getOwner();

            HashToken hashToken = (HashToken) inputToken.get(place2.getId());
            List<INode> toNodes = hashToken.getNodes();
            toNodes.forEach(to -> {
                if (to.equals(cooperatorNode)) {
                    WriteToken toWrite = new WriteToken(request.getId(), request.getKey(), request.getValue());
                    toWrite.setFrom(cooperatorNode);
                    outputToken.addToken(to, place7.getId(), toWrite);
                }
                else {
                    MessageToken toSend = new MessageToken(request.getId(), request.getKey(), request.getValue(), TokenType.WRITE);
                    outputToken.addToken(to, place3.getId(), toSend);
                }
            });
            outputToken.addToken(cooperatorNode, place4.getId(), new CallBackToken(request.getId(), request.getConsistency()));

            return outputToken;
        });

        Transition transition2 = new Transition(2, "transmit");
        transition2.addInPlace(place3).addInPlace(place5);
        transition2.addOutPlace(place5).addOutPlace(place6);
        transition2.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            MessageToken toSend = (MessageToken) inputToken.get(place3.getId());
            MessageToken received = new MessageToken(toSend.getRid(), toSend.getKey(), toSend.getValue(), toSend.getType());
            received.setFrom(toSend.getOwner());
            outputToken.addToken(toSend.getTo(), place6.getId(), received);

            IToken socket = inputToken.get(place5.getId());
            outputToken.addToken(socket.getOwner(), place5.getId(), socket);

            return outputToken;
        });

        Transition transition3 = new Transition(3, "enter writing queue");
        transition3.addInPlace(place6);
        PlacePartition partition2 = new PlacePartition();
        partition2.add(place6.getId());
        transition3.addCondition(partition2, inputToken -> {
            MessageToken received = (MessageToken) inputToken.get(place6.getId());
            return TokenType.WRITE.equals(received.getType());
        });

        transition3.addOutPlace(place7);
        transition3.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            MessageToken received = (MessageToken) inputToken.get(place6.getId());

            WriteToken toWrite = new WriteToken(received.getRid(), received.getKey(), received.getValue());
            toWrite.setFrom(received.getFrom());
            outputToken.addToken(received.getOwner(), place7.getId(), toWrite);

            return outputToken;
        });

        Transition transition4 = new Transition(4, "write");
        transition4.addInPlace(place7);

        Place place8 = new Place(8, "data store", PlaceType.CONSUMELESS);
        Place place9 = new Place(9, "ack queue");

        transition4.addOutPlace(place8).addOutPlace(place9);
        transition4.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            WriteToken toWrite = (WriteToken) inputToken.get(place7.getId());
            outputToken.addToken(toWrite.getOwner(), place8.getId(), toWrite);

            AckToken ack = new AckToken(toWrite.getRid());
            ack.setFrom(toWrite.getFrom());
            outputToken.addToken(toWrite.getOwner(), place9.getId(), ack);

            return outputToken;
        });

        Transition transition5 = new Transition(5, "handle local ack");
        transition5.addInPlace(place4).addInPlace(place9);
        PlacePartition partition3 = new PlacePartition();
        partition3.add(place4.getId());
        partition3.add(place9.getId());
        transition5.addCondition(partition3, inputToken -> {
            CallBackToken callback = (CallBackToken) inputToken.get(place4.getId());
            AckToken ack = (AckToken) inputToken.get(place9.getId());
            return ack.getFrom().equals(ack.getOwner()) && ack.getRid() == callback.getRid();
        });

        transition5.addOutPlace(place4);
        Function<InputToken, OutputToken> ackFunction = (inputToken) -> {
            OutputToken outputToken = new OutputToken();
            CallBackToken callback = (CallBackToken) inputToken.get(place4.getId());
            callback.ack();
            outputToken.addToken(callback.getOwner(), place4.getId(), callback);

            return outputToken;
        };
        transition5.setTransferFunction(ackFunction);

        Transition transition6 = new Transition(6, "enter sending queue");
        transition6.addInPlace(place9);
        PlacePartition partition4 = new PlacePartition();
        partition4.add(place9.getId());
        transition6.addCondition(partition4, inputToken -> {
            AckToken ack = (AckToken) inputToken.get(place9.getId());
            return !ack.getFrom().equals(ack.getOwner());
        });

        transition6.addOutPlace(place3);
        transition6.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            AckToken ack = (AckToken) inputToken.get(place9.getId());
            MessageToken toSend = new MessageToken(ack.getRid(), null, null, TokenType.ACK);
            outputToken.addToken(ack.getOwner(), place3.getId(), toSend);

            return outputToken;
        });

        Place place10 = new Place(10, "ack queue");

        Transition transition7 = new Transition(7, "enter ack queue");
        transition7.addInPlace(place6);
        PlacePartition partition5 = new PlacePartition();
        partition5.add(place6.getId());
        transition7.addCondition(partition5, inputToken -> {
            MessageToken received = (MessageToken) inputToken.get(place6.getId());
            return TokenType.ACK.equals(received.getType());
        });

        transition7.addOutPlace(place10);
        transition7.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            MessageToken received = (MessageToken) inputToken.get(place6.getId());
            AckToken ack = new AckToken(received.getRid());
            outputToken.addToken(received.getOwner(), place10.getId(), ack);

            return outputToken;
        });

        Transition transition8 = new Transition(8, "handle remote ack");
        transition8.addInPlace(place4).addInPlace(place10);
        PlacePartition partition6 = new PlacePartition();
        partition6.add(place4.getId());
        partition6.add(place10.getId());
        transition8.addCondition(partition6, inputToken -> {
            CallBackToken callback = (CallBackToken) inputToken.get(place4.getId());
            AckToken ack = (AckToken) inputToken.get(place10.getId());
            return ack.getRid() == callback.getRid();
        });

        transition8.addOutPlace(place4);
        transition8.setTransferFunction(ackFunction);

        Transition transition9 = new Transition(9, "finish");
        transition9.addInPlace(place4);
        PlacePartition partition7 = new PlacePartition();
        partition7.add(place4.getId());
        transition9.addCondition(partition7, inputToken -> {
            CallBackToken callBack = (CallBackToken) inputToken.get(place4.getId());
            return callBack.getCallback() == 0;
        });

        Place place11 = new Place(11, "response", PlaceType.LOCAL);
        transition9.addOutPlace(place11);
        transition9.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            CallBackToken callBack = (CallBackToken) inputToken.get(place4.getId());
            ResponseToken response = new ResponseToken(callBack.getRid(), ResponseType.SUCCESS);
            outputToken.addToken(callBack.getOwner(), place11.getId(), response);

            return outputToken;
        });

        // TODO Here, timeout is another concept beside the timeout mechanism discussed before.
        // TODO In this situation, timeout process is defined by the user and has its specific process method,
        // TODO and it own way to consume an input token and produce an output token.
        // TODO In our concept, timeout transition just move the original token a timeout place.
        Transition transition10 = new Transition(10, "timeout");
        transition10.addInPlace(place4);
        transition10.addOutPlace(place11);
        transition10.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            CallBackToken callBack = (CallBackToken) inputToken.get(place4.getId());
            ResponseToken response = new ResponseToken(callBack.getRid(), ResponseType.TIMEOUT);
            outputToken.addToken(callBack.getOwner(), place11.getId(), response);

            return outputToken;
        });

        Place place12 = new Place(12, "network partition signal");

        Transition transition11 = new Transition(11, "network partition");
        transition11.addInPlace(place5).addInPlace(place12);
        transition11.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            return outputToken;
        });

        Map<Integer, Place> placeMap = new HashMap<>();
        placeMap.put(1, place1);
        placeMap.put(2, place2);
        placeMap.put(3, place3);
        placeMap.put(4, place4);
        placeMap.put(5, place5);
        placeMap.put(6, place6);
        placeMap.put(7, place7);
        placeMap.put(8, place8);
        placeMap.put(9, place9);
        placeMap.put(10, place10);
        placeMap.put(11, place11);
        placeMap.put(12, place12);

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

        cpn.setPlaces(placeMap);
        cpn.setTransitions(transitionMap);
        instance = new RuntimeFoldingCPN(cpn, nodes);
    }
}
