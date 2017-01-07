//package cn.edu.thu.jcpn.cassandra;
//
//import cn.edu.thu.jcpn.cassandra.token.*;
//import cn.edu.thu.jcpn.cassandra.token.ResponseToken.ResponseType;
//import cn.edu.thu.jcpn.core.cpn.CPN;
//import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
//import cn.edu.thu.jcpn.core.container.Place;
//import cn.edu.thu.jcpn.core.container.Place.PlaceType;
//import cn.edu.thu.jcpn.core.executor.transition.condition.ContainerPartition;
//import cn.edu.thu.jcpn.core.runtime.tokens.INode;
//import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
//import cn.edu.thu.jcpn.core.runtime.tokens.StringNode;
//import cn.edu.thu.jcpn.core.runtime.tokens.UnitToken;
//import cn.edu.thu.jcpn.core.container.Storage;
//import cn.edu.thu.jcpn.core.executor.transition.Transition;
//import cn.edu.thu.jcpn.core.executor.transition.condition.InputToken;
//import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;
//import org.apache.commons.math3.random.EmpiricalDistribution;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.junit.Before;
//
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//import static cn.edu.thu.jcpn.core.container.Place.PlaceType.COMMUNICATING;
//import static cn.edu.thu.jcpn.core.container.Place.PlaceType.LOCAL;
//import static cn.edu.thu.jcpn.core.executor.transition.Transition.TransitionType.TRANSMIT;
//
///**
// * Created by leven on 2016/12/25.
// */
//public class CassandraWriterTest {
//
//    private static Logger logger = LogManager.getLogger();
//
//    private static int SERVER_NUMBER = 5;
//    private static int CLIENT_NUMBER = 20;
//    private static int CONTROLLER_NUMBER = 1;
//
//    private static int REPLICA = 3;
//
//    private RuntimeFoldingCPN instance;
//
//    private Properties empiricalDistributions = new Properties();
//
//    @Before
//    public void initCassandraWriter() throws IOException {
//
//        instance = new RuntimeFoldingCPN();
//
//        //time cost distributions
//        Properties distributions = new Properties();
//        distributions.load(new FileReader(System.getProperty("distributions", "configs/empiricalDistribution")));
//        distributions.forEach((key, value) ->
//                empiricalDistributions.put(key, Arrays.stream(value.toString().split(",")).mapToDouble(Double::valueOf).toArray())
//        );
//
//        EmpiricalDistribution lookupTimeCost2 = new EmpiricalDistribution(100);
//        lookupTimeCost2.load((double[]) empiricalDistributions.get("write_lookup"));
//        EmpiricalDistribution internalNetworkTimeCost2 = new EmpiricalDistribution(100);
//        internalNetworkTimeCost2.load((double[]) empiricalDistributions.get("write_network"));
//        EmpiricalDistribution writeTimeCost2 = new EmpiricalDistribution(100);
//        writeTimeCost2.load((double[]) empiricalDistributions.get("write_locally"));
//        EmpiricalDistribution syncTimeCost2 = new EmpiricalDistribution(100);
//        syncTimeCost2.load((double[]) empiricalDistributions.get("write_response"));
//
//        /**************************************************************************************************************/
//
//        CPN clientCPN = new CPN();
//        clientCPN.setName("clientCPN");
//        clientCPN.setVersion("1.0");
//
//        List<INode> clients = IntStream.rangeClosed(1, CLIENT_NUMBER).
//                mapToObj(x -> new StringNode("client" + x)).collect(Collectors.toList());
//        Place place20 = new Place(20, "client", LOCAL);
//        Place place21 = new Place(21, "network resources", COMMUNICATING);
//
//        Place place1 = new Place(1, "request", LOCAL);
//
//        Transition transition20 = new Transition(20, "make request", TRANSMIT);
//        transition20.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//
//            RequestToken request = (RequestToken) inputToken.get(place20.getId());
//            IToken socket = inputToken.get(place21.getId());
//
//            request.setTimeCost(1);
//            outputToken.addToken(request.getOwner(), place20.getId(), request);
//
//            socket.setTimeCost(1);
//            outputToken.addToken(socket.getOwner(), place21.getId(), socket);
//
//            RequestToken received = new RequestToken(request.getKey(), request.getValue(), request.getConsistency());
//            received.setFrom(socket.getOwner());
//            received.setTimeCost(1);
//            outputToken.addToken(socket.getTo(), place1.getId(), received);
//
//            return outputToken;
//        });
//
//        clientCPN.addPlaces(place20, place21);
//        clientCPN.addTransitions(transition20);
//        instance.addCpn(clientCPN, clients);
//
//        /**************************************************************************************************************/
//
//        CPN serverCPN = new CPN();
//        clientCPN.setName("serverCPN");
//        clientCPN.setVersion("1.0");
//
//        List<INode> servers = IntStream.rangeClosed(1, SERVER_NUMBER).
//                mapToObj(x -> new StringNode("server" + x)).collect(Collectors.toList());
//
//        Storage place2 = new Storage(1, "lookup table");
//        List<INode> cooperateNodes = new ArrayList<>();
//        // initial tokens in place2
//        List<IToken> hashTable = new ArrayList<>();
//        for (int i = 0; i < SERVER_NUMBER; ++i) {
//            cooperateNodes.clear();
//            for (int j = 0; j < REPLICA; ++j) {
//                cooperateNodes.add(servers.get((j + i) % SERVER_NUMBER));
//            }
//            hashTable.add(new HashToken(i, new ArrayList<>(cooperateNodes)));
//        }
//        servers.forEach(node -> place2.addInitTokens(node, hashTable));
//
//        Transition transition1 = new Transition(1, "schedule");
//        transition1.addInPlace(place1).addInPlace(place2);
//        ContainerPartition partition1 = new ContainerPartition(place1.getId(), place2.getId());
//        transition1.addCondition(partition1, inputToken -> {
//            RequestToken request = (RequestToken) inputToken.get(place1.getId());
//            HashToken hashToken = (HashToken) inputToken.get(place2.getId());
//            int hashCode = request.getKey().hashCode() % SERVER_NUMBER;
//            return hashCode == hashToken.getHashCode();
//        });
//
//        Place place3 = new Place(3, "sending queue", PlaceType.COMMUNICATING);
//        Place place4 = new Place(4, "callback", LOCAL);
//
//        Place place5 = new Place(5, "network resources", PlaceType.COMMUNICATING);
//        servers.forEach(owner -> servers.stream().filter(to -> !to.equals(owner)).forEach(to ->
//                place5.addInitToken(null, owner, to, new UnitToken())
//        ));
//
//        Place place6 = new Place(6, "received message", LOCAL);
//        Place place7 = new Place(7, "writing queue", LOCAL);
//
//        transition1.addOutPlace(place3).addOutPlace(place4).addOutPlace(place7);
//        transition1.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//            RequestToken request = (RequestToken) inputToken.get(place1.getId());
//            INode cooperatorNode = request.getOwner();
//            int hashCode = request.getKey().hashCode() % SERVER_NUMBER;
//
//            HashToken hashToken = (HashToken) inputToken.get(hashCode);
//            List<INode> toNodes = hashToken.getNodes();
//
//            long effective = (long) lookupTimeCost2.sample();
//            toNodes.forEach(to -> {
//                if (to.equals(cooperatorNode)) {
//                    WriteToken toWrite = new WriteToken(request.getId(), request.getKey(), request.getValue(), effective);
//                    toWrite.setFrom(cooperatorNode);
//                    outputToken.addToken(to, place7.getId(), toWrite);
//                } else {
//                    MessageToken toSend = new MessageToken(request.getId(), request.getKey(), request.getValue(), TokenType.WRITE, effective);
//                    outputToken.addToken(to, place3.getId(), toSend);
//                }
//            });
//            outputToken.addToken(cooperatorNode, place4.getId(), new CallBackToken(request.getId(), request.getConsistency(), effective));
//
//            return outputToken;
//        });
//
//        Transition transition2 = new Transition(2, "transmit");
//        transition2.addInPlace(place3).addInPlace(place5);
//        transition2.addOutPlace(place5).addOutPlace(place6);
//        transition2.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//            MessageToken toSend = (MessageToken) inputToken.get(place3.getId());
//            MessageToken received = new MessageToken(toSend.getRid(), toSend.getKey(), toSend.getValue(), toSend.getType(), (long) internalNetworkTimeCost2.sample());
//            received.setFrom(toSend.getOwner());
//            outputToken.addToken(toSend.getTo(), place6.getId(), received);
//
//            IToken socket = inputToken.get(place5.getId());
//            outputToken.addToken(socket.getOwner(), place5.getId(), socket);
//
//            return outputToken;
//        });
//
//        Transition transition3 = new Transition(3, "enter writing queue");
//        transition3.addInPlace(place6);
//        ContainerPartition partition2 = new ContainerPartition(place6.getId());
//        transition3.addCondition(partition2, inputToken -> {
//            MessageToken received = (MessageToken) inputToken.get(place6.getId());
//            return TokenType.WRITE.equals(received.getType());
//        });
//
//        transition3.addOutPlace(place7);
//        transition3.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//            MessageToken received = (MessageToken) inputToken.get(place6.getId());
//
//            WriteToken toWrite = new WriteToken(received.getRid(), received.getKey(), received.getValue(), 0);
//            toWrite.setFrom(received.getFrom());
//            outputToken.addToken(received.getOwner(), place7.getId(), toWrite);
//
//            return outputToken;
//        });
//
//        Transition transition4 = new Transition(4, "write");
//        transition4.addInPlace(place7);
//
//        Place place8 = new Place(8, "data store", PlaceType.CONSUMELESS);
//        Place place9 = new Place(9, "ack queue");
//
//        transition4.addOutPlace(place8).addOutPlace(place9);
//        transition4.setTransferFunction(inputToken -> {
//            long effective = (long) writeTimeCost2.sample();
//            OutputToken outputToken = new OutputToken();
//            WriteToken toWrite = (WriteToken) inputToken.get(place7.getId());
//            toWrite.setTimeCost(effective);
//            outputToken.addToken(toWrite.getOwner(), place8.getId(), toWrite);
//
//            AckToken ack = new AckToken(toWrite.getRid(), effective);
//            ack.setFrom(toWrite.getFrom());
//            outputToken.addToken(toWrite.getOwner(), place9.getId(), ack);
//
//            return outputToken;
//        });
//
//        Transition transition5 = new Transition(5, "handle local ack");
//        transition5.addInPlace(place4).addInPlace(place9);
//        ContainerPartition partition3 = new ContainerPartition(place4.getId(), place9.getId());
//        transition5.addCondition(partition3, inputToken -> {
//            CallBackToken callback = (CallBackToken) inputToken.get(place4.getId());
//            AckToken ack = (AckToken) inputToken.get(place9.getId());
//
//            return ack.isLocal() && ack.getRid() == callback.getRid();
//        });
//
//        transition5.addOutPlace(place4);
//        Function<InputToken, OutputToken> ackFunction = (inputToken) -> {
//            OutputToken outputToken = new OutputToken();
//            CallBackToken callback = (CallBackToken) inputToken.get(place4.getId());
//            callback.ack();
//            callback.setTimeCost((long) syncTimeCost2.sample());
//            outputToken.addToken(callback.getOwner(), place4.getId(), callback);
//
//            return outputToken;
//        };
//        transition5.setTransferFunction(ackFunction);
//
//        Transition transition6 = new Transition(6, "enter sending queue");
//        transition6.addInPlace(place9);
//        ContainerPartition partition4 = new ContainerPartition(place9.getId());
//        transition6.addCondition(partition4, inputToken -> {
//            AckToken ack = (AckToken) inputToken.get(place9.getId());
//            return !ack.isLocal();
//        });
//
//        transition6.addOutPlace(place3);
//        transition6.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//            AckToken ack = (AckToken) inputToken.get(place9.getId());
//            MessageToken toSend = new MessageToken(ack.getRid(), null, null, TokenType.ACK, 0);
//            outputToken.addToken(ack.getOwner(), place3.getId(), toSend);
//
//            return outputToken;
//        });
//
//        Place place10 = new Place(10, "ack queue");
//
//        Transition transition7 = new Transition(7, "enter ack queue");
//        transition7.addInPlace(place6);
//        ContainerPartition partition5 = new ContainerPartition(place6.getId());
//        transition7.addCondition(partition5, inputToken -> {
//            MessageToken received = (MessageToken) inputToken.get(place6.getId());
//            return TokenType.ACK.equals(received.getType());
//        });
//
//        transition7.addOutPlace(place10);
//        transition7.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//            MessageToken received = (MessageToken) inputToken.get(place6.getId());
//            AckToken ack = new AckToken(received.getRid(), 0);
//            outputToken.addToken(received.getOwner(), place10.getId(), ack);
//
//            return outputToken;
//        });
//
//        Transition transition8 = new Transition(8, "handle remote ack");
//        transition8.addInPlace(place4).addInPlace(place10);
//        ContainerPartition partition6 = new ContainerPartition(place4.getId(), place10.getId());
//        transition8.addCondition(partition6, inputToken -> {
//            CallBackToken callback = (CallBackToken) inputToken.get(place4.getId());
//            AckToken ack = (AckToken) inputToken.get(place10.getId());
//            return ack.getRid() == callback.getRid();
//        });
//
//        transition8.addOutPlace(place4);
//        transition8.setTransferFunction(ackFunction);
//
//        Transition transition9 = new Transition(9, "finish");
//        transition9.addInPlace(place4);
//        transition9.setPriority(499);
//        ContainerPartition partition7 = new ContainerPartition(place4.getId());
//        transition9.addCondition(partition7, inputToken -> {
//            CallBackToken callBack = (CallBackToken) inputToken.get(place4.getId());
//            return callBack.getCallback() <= 0;
//        });
//
//        Place place11 = new Place(11, "response", LOCAL);
//        transition9.addOutPlace(place11);
//        transition9.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//            CallBackToken callBack = (CallBackToken) inputToken.get(place4.getId());
//            ResponseToken response = new ResponseToken(callBack.getRid(), ResponseType.SUCCESS, 0);
//            outputToken.addToken(callBack.getOwner(), place11.getId(), response);
//
//            return outputToken;
//        });
//
//        // Here, timeout is another concept beside the timeout mechanism discussed before.
//        //  In this situation, timeout process is defined by the user and has its specific process method,
//        //  and its way to consume the input tokens and produce output tokens.
//        //  On the contrary, in our concept, timeout transition just move the original token a timeout place.
//        Transition transition10 = new Transition(10, "timeout");
//        transition10.addInPlace(place4);
//        transition10.setPriority(499);
//        transition10.addOutPlace(place11);
//        transition10.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//            CallBackToken callBack = (CallBackToken) inputToken.get(place4.getId());
//            ResponseToken response = new ResponseToken(callBack.getRid(), ResponseType.TIMEOUT, 0);
//            outputToken.addToken(callBack.getOwner(), place11.getId(), response);
//
//            return outputToken;
//        });
//
//        Place place12 = new Place(12, "network partition signal");
//
//        Transition transition11 = new Transition(11, "network partition");
//        transition11.addInPlace(place5).addInPlace(place12);
//
//        //TODO 这种情况下就不用写output function了, 不过你要检查下,是不是outputFunction为null 将来会报错。
//        /*transition11.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//            return outputToken;
//        });*/
//
//        clientCPN.addPlaces(place1, place2, place3, place4, place5, place6, place7, place8, place9, place10, place11, place12);
//        clientCPN.addTransitions(transition1, transition2, transition3, transition4, transition5, transition6,
//                transition7, transition8, transition9, transition10, transition11);
//        instance = new RuntimeFoldingCPN();
//        instance.addCpn(clientCPN, servers);
//        instance.setMaximumExecutionTime(1000000L * Integer.valueOf(System.getProperty("maxTime", "100")));//us
//    }
//}
