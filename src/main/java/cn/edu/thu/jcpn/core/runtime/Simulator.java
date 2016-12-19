package cn.edu.thu.jcpn.core.runtime;

import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * auto mode:
 * <br>
 * <pre>
 * Simulator engine = new Simulator(foldCPN);
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
public class Simulator {

    private static Logger logger = LogManager.getLogger();

    protected RuntimeFoldingCPN foldingCPN;

    /**
     * // set the maximal execution time of CPN global clock.
     * (because some CPN can execute forever, so we can disrupt the process after maximumExecutionTime.)
     */
    protected long maximumExecutionTime;

    protected Status status;

    protected GlobalClock clock;

    public enum Status {
        RUNNING, STOPPED
    }

    public Simulator(RuntimeFoldingCPN foldingCPN) {
        super();
        this.foldingCPN = foldingCPN;
        maximumExecutionTime = Long.MAX_VALUE;
        status = Status.STOPPED;

        clock = GlobalClock.getInstance();
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
     * If the method compiles the folding cpn, it will register running events at time 0L for
     * all the individual cpn instance, i.e., all the servers.
     *
     */
    public void compile() {
        this.status = Status.RUNNING;
        if (!foldingCPN.isCompiled()) {
            foldingCPN.compile();
            foldingCPN.getOwners().forEach(owner -> clock.addAbsoluteTimepointForRunning(owner, 0L));
        }
    }

    /**
     * if the global clock is less than the expected maximum execution Time, we say that it is not timeout,
     * otherwise it is timeout. If it is timeout, we will change the status to STOPPED
     */
    public boolean isTimeout() {
        if (clock.getTime() >= maximumExecutionTime) {
            this.status = Status.STOPPED;
            return true;
        }
        return false;
    }

    public boolean hasNextTime() {
        return !isTimeout() && clock.hasNextTime();
    }

    /**
     * @return whether the sending queue has events to do.
     */
    public boolean hasNextSendingTime() {
        return !isTimeout() && clock.hasNextSendingTime();
    }

    /**
     * @return whether the running queue has events to do.
     */
    public boolean hasNextRunningTime() {
        return !isTimeout() && clock.hasNextRunningTime();
    }
}
