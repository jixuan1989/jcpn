package cn.edu.thu.jcpn.core.transitions.runtime;

import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.runtime.GlobalClock;
import com.google.common.collect.Iterables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

/**
 * auto mode:
 * <br>
 * <pre>
 * Simulatior engine=new Simulatior(foldCPN);
 * engine.compile();
 * while(engine.hasNextTime()){
 *  System.out.println("next time point:"+engine.getNextTimepoint());
 * 	engine.nextRound();
 * }
 * </pre>
 * <br>
 * manually mode:
 *
 * @author hxd
 */
public class Simulatior {

    private static Logger logger = LogManager.getLogger();

    protected RuntimeFoldingCPN foldingCPN;
    
    protected long maximumExecutionTime; // guarantee the maximal execution time in CPN global clock. (because some CPN can execute forever, so we can disrupt the process after maximumExecutionTime.)
    protected Status status;
    protected GlobalClock clock;
    /**
     * if it is stream mode, the engine will never stop even though there is no transition can fire, because we can use a stream way to add new tokens. However, it will exit if global time >maximal execution time
     */
    protected Mode mode;

    protected Map<Integer, IPlaceMonitor> placeMonitors;
    protected Map<Integer, ITransitionMonitor> transitionMonitors;

    public enum Mode {
        NORMAL, STREAM
    }

    public enum Status {
        RUNNING, STOPPED
    }

    public Simulatior(FoldingCPNInstance cpnInstance, Mode mode) {
        super();
        this.foldingCpnInstance = cpnInstance;
        clock = GlobalClock.getInstance();
        this.mode = mode;
        maximumExecutionTime = Long.MAX_VALUE;
        status = Status.STOPPED;
        placeMonitors = new HashMap<>();
        transitionMonitors = new HashMap<>();
    }

    public void addPlaceMonitor(Integer pid, IPlaceMonitor monitor) {
        placeMonitors.put(pid, monitor);
    }

    public void addTransitionMonitor(Integer tid, ITransitionMonitor monitor) {
        transitionMonitors.put(tid, monitor);
    }

    public long getMaximumExecutionTime() {
        return maximumExecutionTime;
    }

    public void setMaximumExecutionTime(long maximumExecutionTime) {
        this.maximumExecutionTime = maximumExecutionTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * update the status to running. Then, compile its foldingCPNInstance if needed.
     * If the method compiles the folding cpn, it will register running events at time 0L for all the individual cpn instance, i.e., all the servers.
     *
     */
    public void compile() {
        this.status = Status.RUNNING;
        if (!foldingCpnInstance.isCompiled()) {
            foldingCpnInstance.compile();
            foldingCpnInstance.getIndividualInstances().keySet().forEach(owner -> clock.addAbsoluteTimepointForRunning(owner, 0L));
        }
    }

    /**
     * if the global clock is less than the expected maximum execution Time, we say that it is not timeout, otherwise it is timeout.
     * <br> if it is timeout, we will change the status to STOPPED
     */
    public boolean isTimeout() {
        if (GlobalClock.getInstance().getTime() >= maximumExecutionTime) {
            this.status = Status.STOPPED;
            return true;
        }
        return false;
    }

    public boolean hasNextTime() {
        if (!isTimeout()) {
            if (mode.equals(Mode.NORMAL))
                return clock.hasNextTime();
            else // stream mode
                return true;
        }
        return false;
    }

    /**
     * @return whether the sending queue has events to do.
     */
    public boolean hasNextSendingTime() {
        if (!isTimeout()) {
            return clock.hasNextSendingTime();
        }
        return false;
    }

    /**
     * @return whether the running queue has events to do.
     */
    public boolean hasNextRunningTime() {
        if (!isTimeout()) {
            return clock.hasNextRunningTime();
        }
        return false;
    }

//    public boolean hasNextTimepointWithoutQueue() {
//        if (!isTimeout()) {
//            return clock.hasNextTime();
//        }
//        return false;
//    }

    /**
     * used for streaming mode. This method will hang the current thread, until additional tokens, which are out of the system, enter in.
     * <br>A synchronized method.
     */
    protected synchronized void makeSureWeHaveEvents() {
        try {
            logger.debug(() -> "no events in the time line, will wait for additional tokens enter the system...");
            wait();
            logger.trace(() -> "received additional tokens from out of the system.. ");
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }



//    /**
//     * @return
//     * @throws InterruptedException
//     */
//    public long getNextTimepoint() throws InterruptedException {
//        if (!isTimeout()) {
//            if (mode.equals(Mode.STREAM) && !hasNextTimepointWithoutQueue()) {
//                makeSureWeHaveEvents();
//            }
//            Pair<Long, Long> nextTime = clock.nextTime();
//            if (nextTime.getLeft() <= nextTime.getRight()) {
//                return nextTime.getLeft();
//            } else {
//                if (nextTime.getRight() < 0) {
//                    return -nextTime.getRight();
//                } else {
//                    return nextTime.getRight();
//                }
//            }
//        }
//    }

    /**
     * users can use this method to add additional tokens into the system. (i.e., only used for Streaming mode CPN)
     * <br>After adding tokens, the method will wake up hanged threads.</>
     * @param absoluteTime absoluteTime needs to be greater than current time, otherwise we will force setting it to current time +1.
     * @param target
     * @param tokens
     * @return the actually time that the timeline accepts (it is: absoluteTime or currentTime+1)
     * @throws InterruptedException
     */
    public synchronized long pushTokens(Long absoluteTime, ITarget target, Map<Integer, List<IColor>> tokens) throws InterruptedException {
        long time = foldingCpnInstance.addTokens(absoluteTime, target, tokens);
        logger.trace(() -> "will notify all..");
        notifyAll();
        return time;
    }

//    /**
//     * TODO can not understand
//     * @param absoluteTime
//     * @param sout         the sout name.
//     * @param targets      the cpn instances you want to enable.
//     * @return the actually time that the time line accepts (it is: absoluteTime or currentTime+1)
//     * @throws InterruptedException
//     */
//    public synchronized long pushSoutTokens(Long absoluteTime, ITarget sout, ITarget[] targets) throws InterruptedException {
//        //		queue.put(AdditionalTokenPackage.soutTokens(absoluteTime, sout, SoutColor.generateList(targets)));
//        logger.trace(() -> String.format("push a sout token at time %s to target %s. Contents: %s", absoluteTime, sout, Arrays.toString(targets)));
////		logger.info(()->String.format("at time %d, add token to sout: %s",absoluteTime,Arrays.toString(targets)));
//        long time = foldingCpnInstance.addTokens(absoluteTime, sout, Collections.singletonMap(1, SoutColor.generateList(targets)));
//        logger.trace(() -> "will notify all..");
//        notifyAll();
//        return time;
//    }

    protected void reportPlaceBeforeFiring(MixedInputTokenBinding binding, IndividualCPNInstance cpn, Integer tid) {
        if (placeMonitors.size() > 0 && binding.hasSimpleTokens()) {
            binding.getLocalTokens().keySet().stream().filter(pid -> placeMonitors.containsKey(pid)).forEach
                    (pid -> placeMonitors.get(pid).reportWhenConsume(
                                cpn.getOwner(),
                                binding.getLocalTokens().get(pid),
                                Iterables.concat(
                                        ((ILocalTokenOrganizator) cpn.getPlace(pid)).getTestedTokens(),
                                        ((ILocalTokenOrganizator) cpn.getPlace(pid)).getNewlyTokens(),
                                        ((ILocalTokenOrganizator) cpn.getPlace(pid)).getFutureTokens()
                                ),
                                cpn.getTransitions().get(tid).getName(),
                                tid
                    )
            );

        }
        if (placeMonitors.size() > 0 && binding.hasConnectionTokens()) {
            binding.getConnectionTokens().keySet().stream().filter(pid -> placeMonitors.containsKey(pid)).forEach
                    (pid -> placeMonitors.get(pid).reportWhenConsume(
                                cpn.getOwner(), binding.getLocalTokens().get(pid),
                                Iterables.concat(
                                        ((ConnectionPlaceInstance) cpn.getPlace(pid)).getTestedTokens().get(binding.getTarget()),
                                        ((ConnectionPlaceInstance) cpn.getPlace(pid)).getNewlyTokens().get(binding.getTarget()),
                                        ((ConnectionPlaceInstance) cpn.getPlace(pid)).getFutureTokens().get(binding.getTarget())
                                ),
                                cpn.getTransitions().get(tid).getName(),
                                tid
                    )
            );
        }
    }

    protected void reportPlaceAfterFiring(MixedInputTokenBinding binding, IOutputTokenBinding output, IndividualCPNInstance cpn, Integer tid) {
        if (transitionMonitors.containsKey(tid)) {
            transitionMonitors.get(tid).reportAfterFired(cpn.getOwner(), cpn.getTransitions().get(tid).getName(), tid, binding, output);
        }
        if (placeMonitors.size() > 0 && output.hasLocalForIndividualPlace()) {
            output.getLocalForIndividualPlace().keySet().stream().filter(pid -> placeMonitors.containsKey(pid)).forEach
                    (pid -> placeMonitors.get(pid).reportWhenGenerated(
                                cpn.getOwner(),
                                output.getLocalForIndividualPlace().get(pid),
                                Iterables.concat(
                                        ((ILocalTokenOrganizator) cpn.getPlace(pid)).getTestedTokens(),
                                        ((ILocalTokenOrganizator) cpn.getPlace(pid)).getNewlyTokens(),
                                        ((ILocalTokenOrganizator) cpn.getPlace(pid)).getFutureTokens()
                                ),
                                cpn.getTransitions().get(tid).getName(),
                                tid
                    )
            );
        }
        if (placeMonitors.size() > 0 && output.hasLocalForConnectionPlace()) {
            output.getLocalForConnectionPlace().keySet().stream().filter(pid -> placeMonitors.containsKey(pid)).forEach
                    (pid -> placeMonitors.get(pid).reportWhenGenerated(
                                cpn.getOwner(),
                                output.getLocalForConnectionPlace().get(pid),
                                ((ConnectionPlaceInstance) cpn.getPlace(pid)).getTestedTokens(),
                                ((ConnectionPlaceInstance) cpn.getPlace(pid)).getNewlyTokens(),
                                ((ConnectionPlaceInstance) cpn.getPlace(pid)).getFutureTokens(),
                                cpn.getTransitions().get(tid).getName(),
                                tid
                    )
            );
        }
    }
}
