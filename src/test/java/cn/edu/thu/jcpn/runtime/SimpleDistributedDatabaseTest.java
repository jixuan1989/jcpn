package cn.edu.thu.jcpn.runtime;

import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.places.CommunicatingPlace;
import cn.edu.thu.jcpn.core.places.LocalPlace;
import cn.edu.thu.jcpn.core.places.Place;
import cn.edu.thu.jcpn.core.transitions.LocalTransition;
import cn.edu.thu.jcpn.core.transitions.Transition;
import cn.edu.thu.jcpn.core.transitions.TransmitTransition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by leven on 2016/11/21.
 */
public class SimpleDistributedDatabaseTest {

    public static Logger logger = LogManager.getLogger();

    public static int SERVER_NUMBER = 2;

    public CPN cpn;
    public RuntimeFoldingCPN instance;

    @Before
    public void initSimpleDistributedDatabase() {

        cpn = new CPN();
        cpn.setVersion("1.0");

        Map<Integer, Place> placeMap = new HashMap<>();
        // global placeId and placeName.
        LocalPlace place1 = new LocalPlace(1, "local");
        placeMap.put(1, place1);
        LocalPlace place2 = new LocalPlace(2, "received");
        placeMap.put(2, place2);
        CommunicatingPlace place3 = new CommunicatingPlace(3, "toSend");
        placeMap.put(3, place3);
        CommunicatingPlace place4 = new CommunicatingPlace(4, "socket");
        placeMap.put(4, place4);

        Map<Integer, Transition> transitionMap = new HashMap<>();
        // global transitionId and transitionName.
        LocalTransition transition1 = new LocalTransition(1, "execute");
        transitionMap.put(1, transition1);
        TransmitTransition transition2 = new TransmitTransition(2, "transmit");
        transitionMap.put(2, transition2);

        transition1.addInput(place1, 1).addInput(place2, 1);
        transition2.addInput(place3, 1).addInput(place3, 1);

        transition1.addOutput(place1, 1).addOutput(place3, 1);
        transition2.addOutput(place4, 1).addOutput(place2, 1);

        //全局公用一个无意义的UnitColor token值,以减少生成对象次数
        UnitColor unit = new UnitColor();

        int[] serverIds = intArray(SERVER_NUMBER);
        List<ITarget> owners = Arrays.stream(serverIds).mapToObj(x -> new StringOwner("server" + x)).collect(Collectors.toList());

        //p1中保存每个服务器的一个token,用unitColor表示
        owners.forEach(owner -> place1.addInitToken(owner, new UnitColor()));

        IColor token = new ColorMessage(new AtomicInteger(0));
        place3.addInitToken(owners.get(0), owners.get(1), token);

        //p3中保存消息
        //p4中保存每个服务器和别的服务器的套接字token,也用unitColor表示
        owners.forEach(owner -> owners.stream().filter(target -> !target.equals(owner))
                .forEach(target -> place4.addInitToken(owner, target, new UnitColor())));

        //t1,t2写output函数
        int PID_1 = 1;
        int PID_2 = 2;
        int PID_3 = 3;
        int PID_4 = 4;
        transition1.setOutputFunction(
                mixedInputTokenBinding -> { // why not (unit, colorReceived)? // outputTokenBinding no owners? local owner?
                    ColorReceived received = (ColorReceived) mixedInputTokenBinding.getLocalTokens().get(PID_2);
                    IColor sendMessage = new ColorMessage(new AtomicInteger(received.getReceived().intValue() + 1));
                    return LocalOutputTokenBinding.mixBinding(
                            Collections.singletonMap(PID_1, Collections.singletonList(new UnitColor())), // place, place IColor type.
                            1,
                            Collections.singletonMap(PID_3, Collections.singletonMap(received.getFrom(), Collections.singletonList(sendMessage)))
                    );
                }
        );

        transition2.setOutputFunction(
                mixedInputTokenBinding -> {
                    ITarget to = mixedInputTokenBinding.getTarget();
                    ITarget owner = (ITarget) mixedInputTokenBinding.getOwner();
                    ColorMessage toSend = (ColorMessage) mixedInputTokenBinding.getConnectionTokens().get(PID_3);
                    ColorReceived received = new ColorReceived(owner, toSend.getMessage());
                    return RemoteOutputTokenBinding.mixBinding(
                            to,
                            Collections.emptyMap(),
                            Collections.singletonMap(PID_4, Collections.singletonMap(to, Collections.singletonList(new UnitColor()))),
                            1,
                            Collections.singletonMap(PID_2, Collections.singletonList(received)),
                            1
                    );
                }
        );

        cpnet.setPlaces(placeMap);
        cpnet.setTransitions(transitionMap);
        cpnet.preCompile();

        instance = new FoldingCPNInstance(cpnet);
    }

    private int[] intArray(int n) {
        int[] res = new int[n];
        IntStream.rangeClosed(1, n).forEach(i -> res[i - 1] = i);
        return res;
    }

    @Test
    public void test0() throws InterruptedException {
        ManualSimulationEngine engine = new ManualSimulationEngine(instance, NORMAL);
        engine.compile();
        //#!对于每个transition耗时为0的情况下,下面的代码不正确。。需要修正。
        logger.info("hi");
        int count = 0;
        while (engine.hasNextTime()) {

            System.out.println("count :" + count + " -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --");

            if (count++ > 25) {
                break;
            }

            long time = engine.getNextTimepoint();
            Set<IOwner> sendings = engine.getNextSendingInstances(time);
            if (sendings != null) {
                //logger.info("sending size: " + sendings.size());
                sendings.forEach(sender -> logger.info("sender :" + sender));
                sendings.forEach(owner -> {
                    Set<ITarget> targets = engine.getAllSendingTargets(time, owner);
                    targets.forEach(target -> engine.runNextSendingEvent(time, owner, target));
                });
            }

            Set<IOwner> runnings = engine.getNextRunningInstances(time);
            if (runnings != null) {
                // runnings.parallelStream().forEach(owner -> {
                runnings.stream().forEach(owner -> {
                    while (!engine.hasNextSendingTime()) {
                        List<Integer> tids = engine.getAllPossibleFire(owner);
                        if (tids.size() > 0) {
                            Collections.shuffle(tids);
                            tids.forEach(tid -> {

                                        engine.logData();
                                        MixedInputTokenBinding binding = engine.askForBinding(owner, tid);
                                        IOutputTokenBinding output = engine.fire(owner, tid, binding);

                                        if (tid.equals(1)) {
                                            //execute received message
                                            logger.info(() -> String.format("%s handles the message %d from %s", owner.getName(), ((ColorReceived) binding.getLocalTokens().get(2)).getReceived().intValue(), ((ColorReceived) binding.getLocalTokens().get(2)).getFrom().getName()));
                                        } else {
                                            //send a new message to
                                            logger.info(() -> String.format("%s sends the message %d to %s", owner.getName(), ((ColorMessage) binding.getConnectionTokens().get(3)).getMessage().intValue(), binding.getTarget()));
                                        }
                                    }
                            );

                        } else {
                            break;
                        }
                    }
                });
            }
        }
    }
}
