package model.cassandra;

import model.cassandra.token.*;
import model.cassandra.token.ResponseToken.ResponseType;
import cn.edu.thu.jcpn.common.CommonUtil;
import cn.edu.thu.jcpn.core.cpn.CPN;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.container.Place;
import cn.edu.thu.jcpn.core.container.Place.PlaceType;
import cn.edu.thu.jcpn.core.executor.recoverer.Recoverer;
import cn.edu.thu.jcpn.core.monitor.IPlaceMonitor;
import cn.edu.thu.jcpn.core.monitor.IStorageMonitor;
import cn.edu.thu.jcpn.core.monitor.ITransitionMonitor;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import cn.edu.thu.jcpn.core.runtime.tokens.IToken;
import cn.edu.thu.jcpn.core.runtime.tokens.UnitToken;
import cn.edu.thu.jcpn.core.container.Storage;
import cn.edu.thu.jcpn.core.executor.transition.Transition;
import cn.edu.thu.jcpn.core.executor.transition.condition.InputToken;
import cn.edu.thu.jcpn.core.executor.transition.condition.OutputToken;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static cn.edu.thu.jcpn.core.executor.transition.Transition.TransitionType;

/**
 * Created by leven on 2016/12/25.
 */
public class CassandraWriter {

    private static Logger logger = LogManager.getLogger();
    static{
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(System.getProperty("cassandra.config", "release-files/conf-template/exp3.properties")));
        }catch(Exception e){
            logger.error(e);
        }
    }
    private static int SERVER_NUMBER = Integer.valueOf(System.getProperty("server_number", "10"));
    private static int CLIENT_NUMBER =  Integer.valueOf(System.getProperty("client_number", "10"));

    private static int REPLICA = Integer.valueOf(System.getProperty("replica", "3"));
    private static int CONSISTENCY = Integer.valueOf(System.getProperty("consistency", "2"));

    private static int WRITE_THREADS = Integer.valueOf(System.getProperty("write_threads", "2"));
    private static int ACK_THREADS = Integer.valueOf(System.getProperty("ack_threads", "2"));

    private static int MAX_REQUEST=Integer.valueOf(System.getProperty("max_request", "20000"))+10;

    private RuntimeFoldingCPN instance;

    private Properties empiricalDistributions = new Properties();


    public void initCassandraWriter() throws IOException {

        instance = new RuntimeFoldingCPN();
        instance.setVersion("1.0");

        Properties distributions = new Properties();    //time cost distributions
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
        UniformRealDistribution requestInterval = new UniformRealDistribution(500, 501);

        UniformRealDistribution hashCodeSample = new UniformRealDistribution(0, 20);

        /**************************************************************************************************************/

        CPN clientCPN = new CPN("clientCPN");
        List<INode> clients = CommonUtil.generateStringNodes("client", CLIENT_NUMBER);
        List<INode> servers = CommonUtil.generateStringNodes("server", SERVER_NUMBER);

        /**************************************************************************************************************/

        Place place200 = new Place("request", PlaceType.LOCAL);
        Place place201 = new Place("network resources", PlaceType.COMMUNICATING);

        Place place100 = new Place("request", PlaceType.LOCAL);

        clients.forEach(client -> place200.addInitToken(client, new RequestToken(RandomStringUtils.random(4), "value", CONSISTENCY)));
        clients.forEach(client -> servers.forEach(server -> place201.addInitToken(null, client, server, new UnitToken())));

        Transition transition200 = new Transition("make request", TransitionType.TRANSMIT);
        transition200.addInContainer(place200).addInContainer(place201);
        transition200.addOutContainer(place100);
        transition200.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            RequestToken request = (RequestToken) inputToken.get(place200.getId());
            IToken socket = inputToken.get(place201.getId());

            long effective = 555;
            //long effective = (long) requestInterval.sample();
            request.setTimeCost(effective);
            outputToken.addToken(request.getOwner(), place200.getId(), request);
            socket.setTimeCost(effective);
            outputToken.addToken(socket.getOwner(), place201.getId(), socket);

            RequestToken received = new RequestToken(RandomStringUtils.random(4), request.getValue(), request.getConsistency());
            if (received.getId() < MAX_REQUEST) {
                received.setFrom(socket.getOwner());
                received.setTimeCost(effective);
                outputToken.addToken(socket.getTo(), place100.getId(), received);
            }

            return outputToken;
        });

        clientCPN.addContainers(place200, place201);
        clientCPN.addTransitions(transition200);
        instance.addCpn(clientCPN, clients);

        /**************************************************************************************************************/

        CPN serverCPN = new CPN("serverCPN");

        Storage storage101 = new Storage("lookup table");

        Place place102 = new Place("writing queue", PlaceType.LOCAL);
        Place place103 = new Place("callback", PlaceType.LOCAL);
        Place place104 = new Place("sending queue", PlaceType.COMMUNICATING);

        List<INode> cooperateNodes = new ArrayList<>();
        List<IToken> hashTable = new ArrayList<>();
        for (int i = 0; i < SERVER_NUMBER; ++i) {
            cooperateNodes.clear();
            for (int j = 0; j < REPLICA; ++j) {
                cooperateNodes.add(servers.get((j + i) % SERVER_NUMBER));
            }
            hashTable.add(new HashToken(i, new ArrayList<>(cooperateNodes)));
        }
        servers.forEach(node -> storage101.addInitTokens(node, hashTable));

        Transition transition100 = new Transition("schedule", TransitionType.LOCAL);
        transition100.addInContainer(place100).addInContainer(storage101);
        transition100.addCondition(inputToken -> {
            RequestToken request = (RequestToken) inputToken.get(place100.getId());
            HashToken hashToken = (HashToken) inputToken.get(storage101.getId());

//            INode owner = request.getOwner();
//            List<INode> nodes = hashToken.getNodes();
//            INode next = nodes.get(0);
//            return servers.indexOf(next) == (servers.indexOf(owner) + 1) % SERVER_NUMBER;

            int hashCode = request.getKey().hashCode() % SERVER_NUMBER;
            return hashCode == hashToken.getHashCode();
        }, place100, storage101);

        transition100.addOutContainer(place102).addOutContainer(place103).addOutContainer(place104);
        transition100.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            RequestToken request = (RequestToken) inputToken.get(place100.getId());
            INode owner = request.getOwner();
            HashToken hashToken = (HashToken) inputToken.get(storage101.getId());
            List<INode> toNodes = hashToken.getNodes();

            long effective = (long) lookupTimeCost2.sample();
            toNodes.stream().forEach(to -> {
                if (to.equals(owner)) {
                    WriteToken toWrite = new WriteToken(request.getId(), request.getKey(), request.getValue());
                    toWrite.setFrom(owner);
                    toWrite.setTimeCost(effective);
                    outputToken.addToken(owner, place102.getId(), toWrite);
                } else {
                    MessageToken toSend = new MessageToken(request.getId(), request.getKey(), request.getValue(), TokenType.WRITE);
                    toSend.setTo(to);
                    toSend.setTimeCost(effective);
                    outputToken.addToken(owner, place104.getId(), toSend);
                }
            });

            CallBackToken callBack = new CallBackToken(request.getId(), request.getConsistency());
            callBack.setFrom(request.getFrom());
            callBack.setTimeCost(effective);
            outputToken.addToken(owner, place103.getId(), callBack);

            return outputToken;
        });

        /*------------------------------------------------------------------------------------------------------------*/

        Place place105 = new Place("write threads", PlaceType.LOCAL);

        Storage storage106 = new Storage("data store");
        Place place107 = new Place("ack queue", PlaceType.LOCAL);

        servers.forEach(server -> {
            for (int i = 0; i < WRITE_THREADS; ++i)
                place105.addInitToken(server, new UnitToken());
        });
//        storage106.setReplaceStrategy((addedToken, originalToken) -> {
//            WriteToken writeToken1 = (WriteToken) addedToken;
//            WriteToken writeToken2 = (WriteToken) originalToken;
//            //return writeToken1.getKey().equals(writeToken2.getKey());TODO
//            return false;
//        });

        Transition transition101 = new Transition("write", TransitionType.LOCAL);
        transition101.addInContainer(place102).addInContainer(place105);
        transition101.addOutContainer(place105).addOutContainer(storage106).addOutContainer(place107);
        transition101.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            WriteToken toWrite = (WriteToken) inputToken.get(place102.getId());

            long effective = (long) writeTimeCost2.sample();
            IToken writeThread = inputToken.get(place105.getId());
            writeThread.setTimeCost(effective);
            outputToken.addToken(toWrite.getOwner(), place105.getId(), writeThread);

            toWrite.setTimeCost(effective);
            outputToken.addToken(toWrite.getOwner(), storage106.getId(), toWrite);

            AckToken ack = new AckToken(toWrite.getRid());
            ack.setFrom(toWrite.getFrom());
            ack.setTimeCost(effective);
            outputToken.addToken(toWrite.getOwner(), place107.getId(), ack);

            return outputToken;
        });

        /*------------------------------------------------------------------------------------------------------------*/

        Place place108 = new Place("ack threads", PlaceType.LOCAL);
        servers.forEach(server -> {
            for (int i = 0; i < ACK_THREADS; ++i)
                place108.addInitToken(server, new UnitToken());
        });

        Transition transition102 = new Transition("handle local ack", TransitionType.LOCAL);
        transition102.addInContainer(place103).addInContainer(place107).addInContainer(place108);
        transition102.addCondition(inputToken -> {
            CallBackToken callback = (CallBackToken) inputToken.get(place103.getId());
            AckToken ack = (AckToken) inputToken.get(place107.getId());

            return ack.isLocal() && ack.getRid() == callback.getRid();
        }, place103, place107);

        Function<InputToken, OutputToken> ackFunction = (inputToken) -> {
            OutputToken outputToken = new OutputToken();

            CallBackToken callback = (CallBackToken) inputToken.get(place103.getId());
            callback.ack();
            IToken ackThread = inputToken.get(place108.getId());

            long effective = (long) syncTimeCost2.sample();
            callback.setTimeCost(effective);
            outputToken.addToken(callback.getOwner(), place103.getId(), callback);

            ackThread.setTimeCost(effective);
            outputToken.addToken(ackThread.getOwner(), place108.getId(), callback);

            return outputToken;
        };
        transition102.addOutContainer(place103).addOutContainer(place108);
        transition102.setTransferFunction(ackFunction);

        /*------------------------------------------------------------------------------------------------------------*/

        Place place109 = new Place("response", PlaceType.LOCAL);

        Transition transition103 = new Transition("finish", TransitionType.LOCAL);
        transition103.addInContainer(place103);
        transition103.addCondition(inputToken -> {
            CallBackToken callBack = (CallBackToken) inputToken.get(place103.getId());
            return callBack.getCallback() <= 0;
        }, place103);

        transition103.addOutContainer(place109);
        transition103.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();
            CallBackToken callBack = (CallBackToken) inputToken.get(place103.getId());

            long effective = 0;
            ResponseToken response = new ResponseToken(callBack.getRid(), ResponseType.SUCCESS);
            response.setFrom(callBack.getFrom());
            response.setTimeCost(effective);
            outputToken.addToken(callBack.getOwner(), place109.getId(), response);

            return outputToken;
        });

        /*------------------------------------------------------------------------------------------------------------*/

        Recoverer recoverer104 = new Recoverer("timeout");
        recoverer104.addInPlace(place103);
        recoverer104.addOutContainer(place109);
        recoverer104.setTransferFunction(token -> {
            Map<Integer, List<IToken>> outputToken = new HashMap<>();

            CallBackToken callBack = (CallBackToken) token;

            long effective = 0;
            ResponseToken response = new ResponseToken(callBack.getRid(), ResponseType.TIMEOUT);
            response.setFrom(callBack.getFrom());
            response.setTimeCost(effective);
            outputToken.put(place109.getId(), Collections.singletonList(response));

            return outputToken;
        });

        /*------------------------------------------------------------------------------------------------------------*/

        Transition transition105 = new Transition("enter sending queue", TransitionType.LOCAL);
        transition105.addInContainer(place107);
        transition105.addCondition(inputToken -> {
            AckToken ack = (AckToken) inputToken.get(place107.getId());
            return !ack.isLocal();
        }, place107);

        transition105.addOutContainer(place104);
        transition105.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            AckToken ack = (AckToken) inputToken.get(place107.getId());

            long effective = 0;
            MessageToken toSend = new MessageToken(ack.getRid(), null, null, TokenType.ACK);
            toSend.setTo(ack.getFrom());
            toSend.setTimeCost(effective);
            outputToken.addToken(ack.getOwner(), place104.getId(), toSend);

            return outputToken;
        });

        /*------------------------------------------------------------------------------------------------------------*/

        Place place110 = new Place("network resources", PlaceType.COMMUNICATING);
        Place place111 = new Place("received message", PlaceType.LOCAL);

        servers.forEach(owner -> servers.stream().filter(to -> !to.equals(owner)).forEach(to ->
                place110.addInitToken(null, owner, to, new UnitToken())
        ));

        Transition transition106 = new Transition("transmit", TransitionType.TRANSMIT);
        transition106.addInContainer(place104).addInContainer(place110);
        transition106.addOutContainer(place110).addOutContainer(place111);
        transition106.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            MessageToken toSend = (MessageToken) inputToken.get(place104.getId());
            IToken socket = inputToken.get(place110.getId());

            long effective = (long) internalNetworkTimeCost2.sample();
            MessageToken received = new MessageToken(toSend.getRid(), toSend.getKey(), toSend.getValue(), toSend.getType());
            received.setFrom(toSend.getOwner());
            received.setTimeCost(effective);
            outputToken.addToken(toSend.getTo(), place111.getId(), received);

            socket.setTimeCost(effective);
            outputToken.addToken(socket.getOwner(), place110.getId(), socket);

            return outputToken;
        });

        /*------------------------------------------------------------------------------------------------------------*/

        Transition transition107 = new Transition("enter writing queue", TransitionType.LOCAL);
        transition107.addInContainer(place111);
        transition107.addCondition(inputToken -> {
            MessageToken received = (MessageToken) inputToken.get(place111.getId());
            return TokenType.WRITE.equals(received.getType());
        }, place111);

        transition107.addOutContainer(place102);
        transition107.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            MessageToken received = (MessageToken) inputToken.get(place111.getId());

            long effective = 0;
            WriteToken toWrite = new WriteToken(received.getRid(), received.getKey(), received.getValue());
            toWrite.setFrom(received.getFrom());
            toWrite.setTimeCost(effective);
            outputToken.addToken(received.getOwner(), place102.getId(), toWrite);

            return outputToken;
        });

        /*------------------------------------------------------------------------------------------------------------*/

        Place place112 = new Place("ack queue", PlaceType.LOCAL);

        Transition transition108 = new Transition("enter ack queue", TransitionType.LOCAL);
        transition108.addInContainer(place111);
        transition108.addCondition(inputToken -> {
            MessageToken received = (MessageToken) inputToken.get(place111.getId());
            return TokenType.ACK.equals(received.getType());
        }, place111);

        transition108.addOutContainer(place112);
        transition108.setTransferFunction(inputToken -> {
            OutputToken outputToken = new OutputToken();

            MessageToken received = (MessageToken) inputToken.get(place111.getId());

            long effective = 0;
            AckToken ack = new AckToken(received.getRid());
            ack.setTimeCost(effective);
            outputToken.addToken(received.getOwner(), place112.getId(), ack);

            return outputToken;
        });

        /*------------------------------------------------------------------------------------------------------------*/

        Transition transition109 = new Transition("handle remote ack", TransitionType.LOCAL);
        transition109.addInContainer(place103).addInContainer(place108).addInContainer(place112);
        transition109.addCondition(inputToken -> {
            CallBackToken callback = (CallBackToken) inputToken.get(place103.getId());
            AckToken ack = (AckToken) inputToken.get(place112.getId());
            return ack.getRid() == callback.getRid();
        }, place103, place112);

        transition109.addOutContainer(place103).addOutContainer(place108);
        transition109.setTransferFunction(ackFunction);

        /*------------------------------------------------------------------------------------------------------------*/

//        Transition transition110 = new Transition("transmit", TransitionType.TRANSMIT);
//        transition110.addInContainer(place109);
//        transition110.addOutContainer(place200);
//        transition110.setTransferFunction(inputToken -> {
//            OutputToken outputToken = new OutputToken();
//
//            ResponseToken response = (ResponseToken) inputToken.get(place110.getId());
//
//            long effective = 0;
//            RequestToken request = new RequestToken("random me","random me", 2);
//            request.setTimeCost(effective);
//            outputToken.addToken(response.getFrom(), place200.getId(), request);
//            return outputToken;
//        });

        /*------------------------------------------------------------------------------------------------------------*/

        Place place113 = new Place("network partition signal", PlaceType.LOCAL);

        Transition transition111 = new Transition("partition", TransitionType.LOCAL);
        transition111.addInContainer(place110).addInContainer(place113);
        transition111.addCondition(inputToken -> {
            IToken socket = inputToken.get(place110.getId());
            SignalToken signal = (SignalToken) inputToken.get(place113.getId());

            return signal.getSignal().equals(socket.getTo());
        }, place110, place113);

        /*------------------------------------------------------------------------------------------------------------*/

        serverCPN.addContainers(place100, storage101, place102, place103, place104, place105,
                storage106, place107, place108, place109, place110, place111, place112, place113);

        serverCPN.addTransitions(transition100, transition101, transition102,
                transition103, transition105, transition106, transition107, transition108,
                transition109, /*transition110,*/ transition111);
        serverCPN.addRecoverer(recoverer104);
        instance.addCpn(serverCPN, servers);

        /**************************************************************************************************************/

        ITransitionMonitor transitionMonitor100 = new ITransitionMonitor() {
            @Override
            public void reportWhenFiring(long currentTime, INode owner, int transitionId, String transitionName, InputToken inputToken, OutputToken outputToken) {
                StringBuilder stringBuilder = new StringBuilder();

                RequestToken request = (RequestToken) inputToken.get(place100.getId());
                stringBuilder.append(request.getId()).append(",").append(owner).append(",");
                outputToken.values().forEach(cidTokens ->
                        cidTokens.values().forEach(tokens -> {
                            tokens.stream().filter(token -> (token instanceof WriteToken)).
                                    forEach(token -> stringBuilder.append(token.getFrom()).append(","));

                            tokens.stream().filter(token -> (token instanceof MessageToken)).
                                    forEach(token -> stringBuilder.append(token.getTo()).append(","));
                        })
                );

                logger.info(stringBuilder.toString());
            }

//            public void reportWhenFiring(InputToken inputToken, Map<ContainerPartition, List<InputToken>> cache) {
//                StringBuilder stringBuilder = new StringBuilder();
//
//                RequestToken request = (RequestToken) inputToken.get(place100.getId());
//                stringBuilder.append(request.getId()).append(",").append(request.getOwner());
//                stringBuilder.append("\n");
//
//                cache.forEach(((partition, inputTokens) ->
//                    inputTokens.forEach(inputToken1 -> {
//                        if (inputToken1.get(place100.getId()) != null) {
//                            RequestToken token = (RequestToken) inputToken1.get(place100.getId());
//                            stringBuilder.append(token.getId()).append("_").append(token.getTime());
//                        }
//                    })
//                ));
//                logger.info(stringBuilder.toString());
//            }
        };

        IStorageMonitor storageMonitor106 = (time, owner, storageName, token) -> {
            WriteToken temp = (WriteToken) token;
            logger.info(() -> String.format("%d,%s,%d", temp.getRid(), owner, temp.getTime()));
        };

//        IPlaceMonitor placeMonitor100 = new IPlaceMonitor() {
//            @Override
//            public void reportAfterTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens, INode from, int transitionId, String transitionName, Collection<IToken> timeout, Collection<IToken> tested, Collection<IToken> newly, Collection<IToken> future) {
//                RequestToken token = (RequestToken) newTokens.iterator().next();
//                logger.info(() -> String.format("%d,%s,%d,request", token.getTime(), owner, token.getId()));
//            }
//        };
//
        IPlaceMonitor placeMonitor = new IPlaceMonitor() {
            @Override
            public void reportAfterTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens, INode from, int transitionId, String transitionName, Collection<IToken> timeout, Collection<IToken> tested, Collection<IToken> newly, Collection<IToken> future) {
                newTokens.forEach(token -> {
                    if (token instanceof MessageToken) {
                        MessageToken msgToken = (MessageToken) token;
                        if (msgToken.getType().equals(TokenType.WRITE)) {
                            logger.info(() -> String.format("%d,%d,%s,to", msgToken.getRid(), msgToken.getTime(), msgToken.getTo()));
                        }
                    }
                });
            }
        };

        IPlaceMonitor placeMonitor109 = new IPlaceMonitor() {
            @Override
            public void reportAfterTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens, INode from, int transitionId, String transitionName, Collection<IToken> timeout, Collection<IToken> tested, Collection<IToken> newly, Collection<IToken> future) {
                newTokens.forEach(token -> {
                    ResponseToken response = (ResponseToken) token;
                    logger.info(() -> String.format("%d,%d", response.getRid(), response.getTime()));
                });
            }
        };

        IPlaceMonitor placeMonitor102 = new IPlaceMonitor() {
            @Override
            public void reportAfterTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens, INode from, int transitionId, String transitionName, Collection<IToken> timeout, Collection<IToken> tested, Collection<IToken> newly, Collection<IToken> future) {
                newTokens.forEach(token -> {
                    WriteToken writeToken = (WriteToken) token;
                    logger.info(() -> String.format("%d,%d,%s,get", writeToken.getRid(), writeToken.getTime(), owner));
                });
            }

            @Override
            public void reportAfterTokenConsumed(long time, INode owner, int placeId, String placeName, IToken consumed, int transitionId,
                                                 String transitionName, Collection<IToken> timeout, Collection<IToken> tested,
                                                 Collection<IToken> newly, Collection<IToken> future) {
//                StringBuilder stringBuilder = new StringBuilder();
//                tested.forEach(token -> {
//                    WriteToken writeToken1 = (WriteToken) token;
//                    stringBuilder.append(String.format("%d,%d\t", writeToken1.getRid(), writeToken1.getTime()));
//                });

                WriteToken writeToken = (WriteToken) consumed;
//                logger.info(() -> String.format("%d,%d,%d,%s,out--->%s", writeToken.getRid(), writeToken.getTime(), time, owner, stringBuilder.toString()));
                logger.info(() -> String.format("%d,%d,%s,out", writeToken.getRid(), time, owner));
            }
        };

//        IPlaceMonitor placeMonitor102 = new IPlaceMonitor() {
//            @Override
//            public void reportAfterTokensAdded(INode owner, int placeId, String placeName, Collection<IToken> newTokens, INode from, int transitionId, String transitionName, Collection<IToken> timeout, Collection<IToken> tested, Collection<IToken> newly, Collection<IToken> future) {
//                newTokens.forEach(token -> {
//                    WriteToken writeToken = (WriteToken) token;
//                    logger.info(() -> String.format("%d,%d,%d,%s,size", tested.size(), newly.size(), future.size(), owner));
//                });
//            }
//        };

        /**************************************************************************************************************/

        //instance.addMonitor(place100.getId(), placeMonitor100);
        instance.addMonitor(storage106.getId(), storageMonitor106);
//        instance.addMonitor(place102.getId(), placeMonitor102);
        instance.addMonitor(transition100.getId(), transitionMonitor100);
//        instance.addMonitor(place104.getId(), placeMonitor);
        instance.addMonitor(place109.getId(), placeMonitor109);
        instance.setMaximumExecutionTime(1000000L * Integer.valueOf(System.getProperty("maxTime", "100")));//us
    }


    public void test0() throws InterruptedException {
        long start = System.currentTimeMillis();
        while (instance.hasNextTime()) {
            instance.nextRound();
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
    public static void main(String[] args) throws IOException {
        CassandraWriter  cassandraWriter= new CassandraWriter();
        cassandraWriter.initCassandraWriter();
        long start = System.currentTimeMillis();
        while (cassandraWriter.instance.hasNextTime()) {
            cassandraWriter.instance.nextRound();
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
}
