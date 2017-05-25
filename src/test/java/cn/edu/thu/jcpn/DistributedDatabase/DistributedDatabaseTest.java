package cn.edu.thu.jcpn.DistributedDatabase;

import cn.edu.thu.jcpn.DistributedDatabase.token.SignalsToken;
import cn.edu.thu.jcpn.core.container.Place;
import cn.edu.thu.jcpn.core.container.Place.PlaceType;
import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.executor.transition.Transition;
import cn.edu.thu.jcpn.core.executor.transition.Transition.TransitionType;
import cn.edu.thu.jcpn.core.executor.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;
import cn.edu.thu.jcpn.core.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.runtime.tokens.StringNode;
import cn.edu.thu.jcpn.core.runtime.tokens.UnitToken;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by leven on 2017/5/6.
 */
public class DistributedDatabaseTest {

    private static int SLAVES = 2;
    private static int MASTERS = 1;

    private RuntimeFoldingCPN instance;

    @Before
    public void initSimpleDistributedDatabase() {

        instance = new RuntimeFoldingCPN();
        instance.setVersion("1.0");
        instance.setMode(true);

        List<INode> masters = IntStream.rangeClosed(1, MASTERS).mapToObj(x -> new StringNode("master" + x)).collect(Collectors.toList());
        List<INode> slaves = IntStream.rangeClosed(1, SLAVES).mapToObj(x -> new StringNode("slave" + x)).collect(Collectors.toList());

        CPN masterCPN = new CPN("Master");

        Place masterThreads = new Place("Master Threads", PlaceType.LOCAL);
        Place masterSockets = new Place("Master Sockets", PlaceType.COMMUNICATING);

        Place slaveThreads = new Place("Slave Threads", PlaceType.LOCAL);

        Transition request = new Transition("Send an Request", TransitionType.TRANSMIT);
        request.addInContainer(masterThreads).addInContainer(masterSockets);
        request.addOutContainer(masterSockets).addOutContainer(slaveThreads);

        request.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            IToken socket = inputToken.get(masterSockets.getId());
            socket.setTimeCost(1);
            outputToken.addToken(socket.getOwner(), masterSockets.getId(), socket);

            UnitToken wakeup = new UnitToken();
            wakeup.setTimeCost(1);
            outputToken.addToken(socket.getTo(), slaveThreads.getId(), wakeup);

            return outputToken;
        });

        masters.forEach(master -> masterThreads.addInitToken(master, new UnitToken()));

        masters.forEach(master ->
                slaves.forEach(slave ->
                        masterSockets.addInitToken(null, master, slave, new UnitToken())));

        masterCPN.addContainers(masterThreads, masterSockets);
        masterCPN.addTransitions(request);

        instance.addCpn(masterCPN, masters);

        /**********************************************************************************************************************************************/

        CPN slaveCPN = new CPN("Slave");

        Place packagMessages = new Place("Packet Messages", PlaceType.LOCAL);
        Place toSendMessages = new Place("To Send Messages", PlaceType.COMMUNICATING);
        Place receivedMessages = new Place("Received Messages", PlaceType.COMMUNICATING);
        Place receivedAcks = new Place("Received Acks", PlaceType.LOCAL);
        Place respondAcks = new Place("responded  Acks", PlaceType.COMMUNICATING);

        Transition unpackaging = new Transition("Unfolding and Unpackaging a Message", TransitionType.LOCAL);
        unpackaging.addInContainer(slaveThreads).addInContainer(packagMessages);
        unpackaging.addOutContainer(toSendMessages);

        unpackaging.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            SignalsToken packagMessage = (SignalsToken) inputToken.get(packagMessages.getId());

            List<IToken> signals = packagMessage.getSignals();
            signals.forEach(signal -> {
                signal.setTimeCost(1);
                outputToken.addToken(packagMessage.getOwner(), toSendMessages.getId(), signal);
            });
            return outputToken;
        });

        Transition transmitting = new Transition("Transmitting a Message", TransitionType.TRANSMIT);
        transmitting.addInContainer(toSendMessages);
        transmitting.addOutContainer(receivedMessages);

        transmitting.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            IToken signal = inputToken.get(toSendMessages.getId());

            UnitToken receivedSignal = new UnitToken();
            receivedSignal.setFrom(signal.getOwner());
            receivedSignal.setTimeCost(1);
            outputToken.addToken(signal.getTo(), receivedMessages.getId(), receivedSignal);

            return outputToken;
        });

        Transition responding = new Transition("Responding an ack", TransitionType.TRANSMIT);
        responding.addInContainer(receivedMessages);
        responding.addOutContainer(receivedAcks);

        responding.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            IToken receivedSignal = inputToken.get(receivedMessages.getId());

            INode signalOwner = receivedSignal.getFrom();
            INode signalHandler = receivedSignal.getOwner();

            UnitToken ack = new UnitToken();
            ack.setOwner(signalOwner);
            ack.setFrom(signalHandler);
            ack.setTimeCost(1);
            outputToken.addToken(signalOwner, receivedAcks.getId(), ack);

            return outputToken;
        });

        Transition ackHandling = new Transition("handling an ack", TransitionType.LOCAL);
        ackHandling.addInContainer(receivedAcks).addInContainer(respondAcks);
        ackHandling.addOutContainer(respondAcks);

        ackHandling.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            SignalsToken acks = (SignalsToken) inputToken.get(respondAcks.getId());
            IToken respondAck = inputToken.get(receivedAcks.getId());

            INode owner = acks.getOwner();
            INode from = respondAck.getFrom();

            SignalsToken newAcks = new SignalsToken(acks);
            UnitToken ack = new UnitToken();
            ack.setTo(from);
            ack.setOwner(owner);
            newAcks.add(ack);
            newAcks.setTimeCost(1);
            outputToken.addToken(owner, respondAcks.getId(), newAcks);

            return outputToken;
        });

        Transition packaging = new Transition("Folding and Packaging a Message", TransitionType.TRANSMIT);
        packaging.addInContainer(respondAcks);
        packaging.addOutContainer(masterThreads).addOutContainer(packagMessages).addOutContainer(respondAcks);
        packaging.addCondition(inputToken -> {
            SignalsToken respondAck = (SignalsToken) inputToken.get(respondAcks.getId());

            return respondAck.getSignals().size() == SLAVES - 1;
        }, respondAcks);

        packaging.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            SignalsToken respondAck = (SignalsToken) inputToken.get(respondAcks.getId());

            INode owner = respondAck.getOwner();

            SignalsToken packageMessage = new SignalsToken(respondAck);
            packageMessage.setTimeCost(1);
            outputToken.addToken(owner, packagMessages.getId(), packageMessage);

            SignalsToken acks = new SignalsToken();
            acks.setTimeCost(1);
            outputToken.addToken(owner, respondAcks.getId(), acks);

            UnitToken masterThread = new UnitToken();
            masterThread.setTimeCost(1);
            outputToken.addToken(masters.get(0), masterThreads.getId(), masterThread);

            return outputToken;
        });

        slaves.forEach(slave -> packagMessages.addInitToken(slave, new SignalsToken(slave, slaves)));
        slaves.forEach(slave -> respondAcks.addInitToken(slave, new SignalsToken(slave)));

        slaveCPN.addContainers(slaveThreads, packagMessages, toSendMessages, receivedMessages, respondAcks, receivedAcks);
        slaveCPN.addTransitions(unpackaging, transmitting, responding, packaging, ackHandling);
        instance.addCpn(slaveCPN, slaves);

//        instance.addMonitor(waitings.getId(), placeMonitor);
//        instance.addMonitor(actives.getId(), placeMonitor);
//        instance.addMonitor(unuseds.getId(), placeMonitor);
//        instance.addMonitor(passives.getId(), placeMonitor);
//        instance.addMonitor(inactives.getId(), placeMonitor);
//        instance.addMonitor(receiveds.getId(), placeMonitor);
//        instance.addMonitor(performings.getId(), placeMonitor);
//        instance.addMonitor(sents.getId(), placeMonitor);
//        instance.addMonitor(acknowledgeds.getId(), placeMonitor);

        ITransitionMonitor transitionMonitor = new ITransitionMonitor() {
            @Override
            public void reportWhenFiring(long currentTime, INode owner, int transitionId, String transitionName, InputToken inputToken, OutputToken outputToken) {
                System.out.println(owner + "'s " + transitionName + " is fired");
            }
        };

        instance.addMonitor(unpackaging.getId(), transitionMonitor);
        instance.addMonitor(packaging.getId(), transitionMonitor);
        instance.addMonitor(responding.getId(), transitionMonitor);
        instance.addMonitor(transmitting.getId(), transitionMonitor);
        instance.addMonitor(request.getId(), transitionMonitor);
        instance.addMonitor(ackHandling.getId(), transitionMonitor);

    }

    @Test
    public void test0() throws InterruptedException {
        int time = 10000;
        long start = System.currentTimeMillis();
        //int count = 0;
        while (instance.hasNextTime()) {
            instance.nextRound(start, time);
            //++count;
            //if (count++ == Integer) break;
        }
        //long end = System.currentTimeMillis();
        //System.out.println(time + "," + (end - start));
    }
}
