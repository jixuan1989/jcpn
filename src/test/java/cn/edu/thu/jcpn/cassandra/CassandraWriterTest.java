package cn.edu.thu.jcpn.cassandra;

import cn.edu.thu.jcpn.cassandra.token.*;
import cn.edu.thu.jcpn.cassandra.token.ResponseToken.ResponseType;
import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.container.Place;
import cn.edu.thu.jcpn.core.container.Place.PlaceType;
import cn.edu.thu.jcpn.core.executor.recoverer.Recoverer;
import cn.edu.thu.jcpn.core.executor.transition.condition.ContainerPartition;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.runtime.tokens.StringNode;
import cn.edu.thu.jcpn.core.runtime.tokens.UnitToken;
import cn.edu.thu.jcpn.core.container.Storage;
import cn.edu.thu.jcpn.core.executor.transition.Transition;
import cn.edu.thu.jcpn.core.executor.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.edu.thu.jcpn.core.executor.transition.Transition.TransitionType;

/**
 * Created by leven on 2016/12/25.
 */
public class CassandraWriterTest {

    private static Logger logger = LogManager.getLogger();

    private static int SERVER_NUMBER = 3;
    private static int CLIENT_NUMBER = 1;

    private static int REPLICA = 3;
    private static int CONSISTENCY = 2;

    private RuntimeFoldingCPN instance;

    private Properties empiricalDistributions = new Properties();

    @Before
    public void initCassandraWriter() throws IOException {

        instance = new RuntimeFoldingCPN();
        instance.setVersion("1.0");
        //time cost distributions
        Properties distributions = new Properties();
        distributions.load(new FileReader(System.getProperty("distributions", "examples/cassandra/configs/empiricalDistribution")));
        distributions.forEach((key, value) ->
                empiricalDistributions.put(key, Arrays.stream(value.toString().split(",")).mapToDouble(Double::valueOf).toArray())
        );

        EmpiricalDistribution lookupTimeCost2 = new EmpiricalDistribution(100);
        lookupTimeCost2.load((double[]) empiricalDistributions.get("write_lookup"));
        EmpiricalDistribution internalNetworkTimeCost2 = new EmpiricalDistribution(100);
        internalNetworkTimeCost2.load((double[]) empiricalDistributions.get("write_network"));
        EmpiricalDistribution writeTimeCost2 = new EmpiricalDistribution(100);
        writeTimeCost2.load((double[]) empiricalDistributions.get("write_locally"));
        EmpiricalDistribution syncTimeCost2 = new EmpiricalDistribution(100);
        syncTimeCost2.load((double[]) empiricalDistributions.get("write_response"));
        UniformRealDistribution requestInterval = new UniformRealDistribution(10, 12);
        /**************************************************************************************************************/

        CPN clientCPN = new CPN("clientCPN");

        List<INode> clients = IntStream.rangeClosed(1, CLIENT_NUMBER).
                mapToObj(x -> new StringNode("client" + x)).collect(Collectors.toList());

        List<INode> servers = IntStream.rangeClosed(1, SERVER_NUMBER).
                mapToObj(x -> new StringNode("server" + x)).collect(Collectors.toList());


        Place place200 = new Place(200, "request", PlaceType.LOCAL);
        for (int i = 0; i < 10; ++i) {
            clients.forEach(client -> place200.addInitToken(client, new RequestToken("key", "value", CONSISTENCY)));
        }

        Place place201 = new Place(201, "network resources", PlaceType.COMMUNICATING);
        clients.forEach(client -> servers.forEach(server -> place201.addInitToken(null, client, server, new UnitToken())));

        Transition transition200 = new Transition(200, "make request", TransitionType.TRANSMIT);
        transition200.addInContainer(place200).addInContainer(place201);

        Place place100 = new Place(100, "request", PlaceType.LOCAL);
        transition200.addOutContainer(place100);
        transition200.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            RequestToken request = (RequestToken) inputToken.get(place200.getId());
            IToken socket = inputToken.get(place201.getId());

            long effective = (long) requestInterval.sample();
            //return back to 200 and 201
//            request.setTimeCost(effective);
//            outputToken.addToken(request.getOwner(), place200.getId(), request);
            socket.setTimeCost(effective);
            outputToken.addToken(socket.getOwner(), place201.getId(), socket);

            RequestToken received = new RequestToken(request.getKey(), request.getValue(), request.getConsistency());
            received.setFrom(socket.getOwner());
            received.setTimeCost(effective);
            outputToken.addToken(socket.getTo(), place100.getId(), received);

            return outputToken;
        });

        clientCPN.addContainers(place200, place201);
        clientCPN.addTransitions(transition200);
        instance.addCpn(clientCPN, clients);

        /**************************************************************************************************************/

        CPN serverCPN = new CPN("serverCPN");

        Storage storage101 = new Storage(101, "lookup table");
        List<INode> cooperateNodes = new ArrayList<>();
        // initial tokens in the storage 101, and place 104
        List<IToken> hashTable = new ArrayList<>();
        for (int i = 0; i < SERVER_NUMBER; ++i) {
            cooperateNodes.clear();
            for (int j = 0; j < REPLICA; ++j) {
                cooperateNodes.add(servers.get((j + i) % SERVER_NUMBER));
            }
            hashTable.add(new HashToken(i, new ArrayList<>(cooperateNodes)));
        }
        servers.forEach(node -> storage101.addInitTokens(node, hashTable));

        Place place104 = new Place(104, "network resources", PlaceType.COMMUNICATING);
        servers.forEach(owner -> servers.stream().filter(to -> !to.equals(owner)).forEach(to ->
                place104.addInitToken(null, owner, to, new UnitToken())
        ));


        Transition transition100 = new Transition(100, "schedule", TransitionType.LOCAL);
        transition100.addInContainer(place100).addInContainer(storage101);
        ContainerPartition partition1 = new ContainerPartition(place100.getId(), storage101.getId());
        transition100.addCondition(partition1, inputToken -> {
            RequestToken request = (RequestToken) inputToken.get(place100.getId());
            HashToken hashToken = (HashToken) inputToken.get(storage101.getId());
            int hashCode = request.getKey().hashCode() % SERVER_NUMBER;
            return hashCode == hashToken.getHashCode();
        });

        Place place102 = new Place(102, "sending queue", PlaceType.COMMUNICATING);
        Place place103 = new Place(103, "callback", PlaceType.LOCAL);

        Place place105 = new Place(105, "received message", PlaceType.LOCAL);
        Place place106 = new Place(106, "writing queue", PlaceType.LOCAL);

        transition100.addOutContainer(place102).addOutContainer(place103).addOutContainer(place106);
        transition100.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            RequestToken request = (RequestToken) inputToken.get(place100.getId());
            INode owner = request.getOwner();
            HashToken hashToken = (HashToken) inputToken.get(storage101.getId());
            List<INode> toNodes = hashToken.getNodes();

            long effective = (long) lookupTimeCost2.sample();
            toNodes.forEach(to -> {
                if (to.equals(owner)) {
                    WriteToken toWrite = new WriteToken(request.getId(), request.getKey(), request.getValue());
                    toWrite.setTimeCost(effective);
                    toWrite.setFrom(owner);
                    outputToken.addToken(owner, place106.getId(), toWrite);
                } else {
                    MessageToken toSend = new MessageToken(request.getId(), request.getKey(), request.getValue(), TokenType.WRITE);
                    toSend.setTimeCost(effective);
                    toSend.setTo(to);
                    outputToken.addToken(owner, place102.getId(), toSend);
                }
            });

            CallBackToken callBack = new CallBackToken(request.getId(), request.getConsistency());
            callBack.setTimeCost(effective);
            callBack.setFrom(request.getFrom());
            outputToken.addToken(owner, place103.getId(), callBack);

            return outputToken;
        });

        Transition transition101 = new Transition(101, "transmit", TransitionType.TRANSMIT);
        transition101.addInContainer(place102).addInContainer(place104);
        transition101.addOutContainer(place104).addOutContainer(place105);
        transition101.setTransferFunction(inputToken -> {
            long effective = (long) internalNetworkTimeCost2.sample();
            OutputToken outputToken = new OutputToken();
            MessageToken toSend = (MessageToken) inputToken.get(place102.getId());
            MessageToken received = new MessageToken(toSend.getRid(), toSend.getKey(), toSend.getValue(), toSend.getType());
            received.setTimeCost(effective);
            received.setFrom(toSend.getOwner());
            outputToken.addToken(toSend.getTo(), place105.getId(), received);

            IToken socket = inputToken.get(place104.getId());
            socket.setTimeCost(effective);
            outputToken.addToken(socket.getOwner(), place104.getId(), socket);

            return outputToken;
        });

        Transition transition102 = new Transition(102, "enter writing queue", TransitionType.LOCAL);
        transition102.addInContainer(place105);
        ContainerPartition partition2 = new ContainerPartition(place105.getId());
        transition102.addCondition(partition2, inputToken -> {
            MessageToken received = (MessageToken) inputToken.get(place105.getId());
            return TokenType.WRITE.equals(received.getType());
        });

        transition102.addOutContainer(place106);
        transition102.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            MessageToken received = (MessageToken) inputToken.get(place105.getId());

            WriteToken toWrite = new WriteToken(received.getRid(), received.getKey(), received.getValue());
            toWrite.setTimeCost(0);
            toWrite.setFrom(received.getFrom());
            outputToken.addToken(received.getOwner(), place106.getId(), toWrite);

            return outputToken;
        });

        Transition transition103 = new Transition(103, "write", TransitionType.LOCAL);
        transition103.addInContainer(place106);

        Storage storage108 = new Storage(108, "data store");
        storage108.setReplaceStrategy((addedToken, originalToken) -> {
            WriteToken writeToken1 = (WriteToken) addedToken;
            WriteToken writeToken2 = (WriteToken) originalToken;
            //return writeToken1.getKey().equals(writeToken2.getKey());TODO
            return false;
        });

        Place place109 = new Place(109, "ack queue", PlaceType.LOCAL);

        transition103.addOutContainer(storage108).addOutContainer(place109);
        transition103.setTransferFunction(inputToken -> {
            long effective = (long) writeTimeCost2.sample();
            OutputToken outputToken = new OutputToken();
            WriteToken toWrite = (WriteToken) inputToken.get(place106.getId());
            toWrite.setTimeCost(effective);
            outputToken.addToken(toWrite.getOwner(), storage108.getId(), toWrite);

            AckToken ack = new AckToken(toWrite.getRid());
            ack.setTimeCost(effective);
            ack.setFrom(toWrite.getFrom());
            outputToken.addToken(toWrite.getOwner(), place109.getId(), ack);

            return outputToken;
        });

        Transition transition104 = new Transition(104, "handle local ack", TransitionType.LOCAL);
        transition104.addInContainer(place103).addInContainer(place109);
        ContainerPartition partition3 = new ContainerPartition(place103.getId(), place109.getId());
        transition104.addCondition(partition3, inputToken -> {
            CallBackToken callback = (CallBackToken) inputToken.get(place103.getId());
            AckToken ack = (AckToken) inputToken.get(place109.getId());

            return ack.isLocal() && ack.getRid() == callback.getRid();
        });

        transition104.addOutContainer(place103);
        Function<InputToken, OutputToken> ackFunction = (inputToken) -> {
            OutputToken outputToken = new OutputToken();
            CallBackToken callback = (CallBackToken) inputToken.get(place103.getId());
            callback.ack();
            callback.setTimeCost((long) syncTimeCost2.sample());
            outputToken.addToken(callback.getOwner(), place103.getId(), callback);

            return outputToken;
        };
        transition104.setTransferFunction(ackFunction);

        Transition transition105 = new Transition(105, "enter sending queue", TransitionType.LOCAL);
        transition105.addInContainer(place109);
        ContainerPartition partition4 = new ContainerPartition(place109.getId());
        transition105.addCondition(partition4, inputToken -> {
            AckToken ack = (AckToken) inputToken.get(place109.getId());
            return !ack.isLocal();
        });

        transition105.addOutContainer(place102);
        transition105.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            AckToken ack = (AckToken) inputToken.get(place109.getId());
            MessageToken toSend = new MessageToken(ack.getRid(), null, null, TokenType.ACK);
            toSend.setTimeCost(0);
            toSend.setTo(ack.getFrom());
            outputToken.addToken(ack.getOwner(), place102.getId(), toSend);

            return outputToken;
        });


        Transition transition106 = new Transition(106, "enter ack queue", TransitionType.LOCAL);
        transition106.addInContainer(place105);
        ContainerPartition partition5 = new ContainerPartition(place105.getId());
        transition106.addCondition(partition5, inputToken -> {
            MessageToken received = (MessageToken) inputToken.get(place105.getId());
            return TokenType.ACK.equals(received.getType());
        });

        Place place107 = new Place(107, "ack queue", PlaceType.LOCAL);

        transition106.addOutContainer(place107);
        transition106.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            MessageToken received = (MessageToken) inputToken.get(place105.getId());
            AckToken ack = new AckToken(received.getRid());
            ack.setTimeCost(0);
            outputToken.addToken(received.getOwner(), place107.getId(), ack);

            return outputToken;
        });

        Transition transition107 = new Transition(107, "handle remote ack", TransitionType.LOCAL);
        transition107.addInContainer(place103).addInContainer(place107);
        ContainerPartition partition6 = new ContainerPartition(place103.getId(), place107.getId());
        transition107.addCondition(partition6, inputToken -> {
            CallBackToken callback = (CallBackToken) inputToken.get(place103.getId());
            AckToken ack = (AckToken) inputToken.get(place107.getId());
            return ack.getRid() == callback.getRid();
        });

        transition107.addOutContainer(place103);
        transition107.setTransferFunction(ackFunction);

        Transition transition108 = new Transition(108, "finish", TransitionType.LOCAL);
        transition108.addInContainer(place103);
        transition108.setPriority(499);//TODO
        ContainerPartition partition7 = new ContainerPartition(place103.getId());
        transition108.addCondition(partition7, inputToken -> {
            CallBackToken callBack = (CallBackToken) inputToken.get(place103.getId());
            return callBack.getCallback() <= 0;
        });

        Place place110 = new Place(110, "response", PlaceType.LOCAL);
        transition108.addOutContainer(place110);
        transition108.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            CallBackToken callBack = (CallBackToken) inputToken.get(place103.getId());
            ResponseToken response = new ResponseToken(callBack.getRid(), ResponseType.SUCCESS);
            response.setTimeCost(0);
            response.setFrom(callBack.getFrom());
            outputToken.addToken(callBack.getOwner(), place110.getId(), response);

            return outputToken;
        });

        Recoverer recoverer109 = new Recoverer(109, "timeout");
        recoverer109.addInPlace(place103);
        recoverer109.addOutContainer(place110);
        recoverer109.setTransferFunction(token -> {
            Map<Integer, List<IToken>> outputToken = new HashMap<>();
            CallBackToken callBack = (CallBackToken) token;
            ResponseToken response = new ResponseToken(callBack.getRid(), ResponseType.TIMEOUT);
            response.setTimeCost(0);
            response.setFrom(callBack.getFrom());
            outputToken.put(place110.getId(), Collections.singletonList(response));

            return outputToken;
        });

        Transition transition110 = new Transition(110, "transmit", TransitionType.TRANSMIT);
        transition110.addInContainer(place110);
//        transition110.addOutContainer(place200);
//        transition110.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//            ResponseToken response = (ResponseToken) inputToken.get(place110.getId());
//            RequestToken request = new RequestToken("random me","random me",2);//TODO
//            outputToken.addToken(response.getFrom(), place200.getId(), request);
//
//            return outputToken;
//        });

        Place place111 = new Place(112, "network partition signal", PlaceType.LOCAL);

        Transition transition111 = new Transition(111, "partition", TransitionType.LOCAL);
        transition111.addInContainer(place104).addInContainer(place111);
        ContainerPartition partition8 = new ContainerPartition(place104.getId(), place111.getId());
        transition111.addCondition(partition8, inputToken -> {
            SignalToken signal = (SignalToken) inputToken.get(place111.getId());
            IToken socket = inputToken.get(place104.getId());

            return signal.getSignal().equals(socket.getTo());
        });


        serverCPN.addContainers(place100, storage101, place102, place103, place104, place105, place106, storage108, place109, place107, place110, place111);
        serverCPN.addTransitions(transition100, transition101, transition102, transition103, transition104, transition105,
                transition106, transition107, transition108, transition111);
        serverCPN.addRecoverer(recoverer109);
        instance.addCpn(serverCPN, servers);

        ITransitionMonitor transitionMonitor = (time, owner, transitionId, transitionName, inputToken, outputToken) -> {
            System.out.println("test----------");
            logger.info(() -> String.format("[%d] owner %s executes %d (%s)", time, owner, transitionId, transitionName));
        };
        serverCPN.getTransitions().forEach((tid, transition) -> instance.addMonitor(tid, transitionMonitor));
        instance.setMaximumExecutionTime(1000000L * Integer.valueOf(System.getProperty("maxTime", "100")));//us
    }

    @Test
    public void test0() throws InterruptedException {
        int count = 0;
        while (instance.hasNextTime()) {
            instance.nextRound();
            if (count++ == 666) break;
        }
    }
}
