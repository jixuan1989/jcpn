package cn.edu.thu.jcpn.core.runtime;

import cn.edu.thu.jcpn.common.Pair;
import cn.edu.thu.jcpn.core.cpn.runtime.RuntimeFoldingCPN;
import cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType.RUNNING;

/**
 * manually mode:
 * <pre>
 * ManualSimulationEngine engine = new ManualSimulationEngine(foldCPN);
 * engine.compile();
 * while (engine.hasNextTime()) {
 *  Long time = engine.getNextTimepoint();
 *  Set&lt;Object&gt; sendings = engine.getNextSendingInstances(time);
 *  if (sendings != null) {
 *      sendings.forEach(owner -> {
 *          Set&lt;Object&gt; targets = engine.getAllSendingTargets(time, owner);
 *          targets.forEach(target -> engine.runNextSendingEvent(time, owner, target));
 *      });
 *  }
 *
 *  Set&lt;Object&gt; runnings = engine.getNextRunningInstances(time);
 *  if (runnings != null) {
 *      runnings.forEach(owner -> {
 *          while (true) {
 *  	        List&lt;Integer&gt; tids = engine.getAllPossibleFire(owner);
 *              if (tids.size() > 0) {
 *  	            Collections.shuffle(tids);
 *                  MixedInputTokenBinding binding = engine.askForBinding(owner,tids.get(0));
 *                  IOutputTokenBinding out = engine.fire(owner,tids.get(0),binding);
 *              } else {
 *                  break;
 *              }
 *          }
 *      });
 * }
 * </pre>
 * <br>Notice, the class may not execute a time point from getNextTimepoint(), because the latest time may have been modified.
 * <br> if you want to guarantee that, use synchronized keyword to wrap this object and code snippet from getNextTimepoint to next****() method
 *
 * @author hxd
 */
public class ManualSimulator extends Simulator {

    private static Logger logger = LogManager.getLogger();

    public ManualSimulator(RuntimeFoldingCPN foldingCPN) {
        super(foldingCPN);
    }

    /**
     * Notice: you MUST run runNextSendingEvent, if you have called getNextSendingInstances.
     * It is because these events have been cleaned from background schedule.
     * <br> this method may run a more earlier time event.
     *
     * @return null if the time has no sending events
     * @throws InterruptedException
     */
    public Set<IOwner> getNextSendingInstances() throws InterruptedException {
        if(!super.hasNextTime()) {
            logger.info("no sending events in the timeline");
            return null;
        }

        Pair<EventType, Entry<Long, Map<IOwner, Object>>> entry = clock.timeElapse();
        if (entry == null) {
            logger.info("no sending events in the timeline");
            return null;
        }

        if (entry.getLeft() == RUNNING) {
            logger.warn("want to get a sending event, but get a running event" + entry);
            return null;
        } else {
            foldingCPN.applyCachedSendingTasks(entry.getRight().getKey());
            return entry.getRight().getValue().keySet();
        }
    }

    public void runNextSendingEvent(Long absolutiveTime, IOwner owner, ITarget target) {
        foldingCpnInstance.getIndividualInstances().get(owner).sendRemoteNewlyTokens(absolutiveTime, target, foldingCpnInstance);
    }

    /**
     * Notice: you MUST run runNextRunningEvent, if you have called getNextRunningInstances.
     * It is because these events have been cleaned from background schedule.
     *
     * @return null if the time has no running events
     */
    public Set<IOwner> getNextRunningInstances() {
        if(!hasNextTime()) {
            logger.info("no running events in the timeline");
            return null;
        }

        Pair<EventType, Entry<Long, Map<IOwner, Object>>> entry = clock.timeElapse();
        if (entry == null) {
            logger.info("no running events in the timeline");
            return null;
        }

        if (entry.getLeft() == EventType.SENDING) {
            logger.warn("want to get a runnxing event, but get a sending event" + entry);
            return null;
        } else {
            return entry.getRight().getValue().keySet();
        }
    }

    public Set<ITarget> getAllSendingTargets(Long time, IOwner owner) {
        Map<Long, Map<ITarget, Map<Integer, List<IColor>>>> map = foldingCpnInstance.getIndividualInstances().get(owner).getRemoteSchedule();
        if (!map.containsKey(time)) {
            return null;
        }
        return map.get(time).keySet();
    }

    public List<Integer> getAllPossibleFire(IOwner owner) {
        IndividualCPNInstance instance = foldingCpnInstance.getIndividualInstances().get(owner);
        instance.checkNewlyTokensAndMarkAsTested();
        if (instance.canFire()) {
            return instance.getCanFireTransitions();
        } else {
            return Collections.emptyList();
        }
    }

    public MixedInputTokenBinding askForBinding(IOwner owner, Integer tid) {
        return foldingCpnInstance.getIndividualInstances().get(owner).askForFire(tid);
    }

    public IOutputTokenBinding fire(IOwner owner, Integer tid, MixedInputTokenBinding binding) {
        IndividualCPNInstance instance = foldingCpnInstance.getIndividualInstances().get(owner);
        reportPlaceBeforeFiring(binding, instance, tid);
        if (instance.trytoGetTokensFromPlaces(binding)) {
            instance.removeTokensFromAllTransitionsCache(binding);
            IOutputTokenBinding output = instance.fire(tid, binding);
            if (output != null)
                instance.addLocalNewlyTokensAndScheduleNextTime(output);
            instance.checkNewlyTokensAndMarkAsTested();
            reportPlaceAfterFiring(binding, output, instance, tid);
            return output;
        }
        return null;
    }

    public void logData() {
        FoldingCPNInstance cpn = this.foldingCpnInstance;
        cpn.logStatus();
    }

}
