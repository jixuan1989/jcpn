package cn.edu.thu.jcpn.core.runtime;

import cn.edu.thu.jcpn.common.Pair;
import cn.edu.thu.jcpn.core.runtime.tokens.IOwner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType.RUNNING;
import static cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType.SENDING;

public class GlobalClock {

    private static Logger logger = LogManager.getLogger();

    private static GlobalClock globalClock = new GlobalClock();
    private long time = 0L;

    public enum EventType {
        RUNNING, SENDING
    }

    public static GlobalClock getInstance() {
        return globalClock;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    // <time, owner, redundant>, redundant value is useless, because java skipListMap doesn't have a concurrent HashSet implementation.
    ConcurrentSkipListMap<Long, Map<IOwner, Object>> timelineForRunning = new ConcurrentSkipListMap<>();
    ConcurrentSkipListMap<Long, Map<IOwner, Object>> timelineForSending = new ConcurrentSkipListMap<>();

    public void addAbsoluteTimepointForRunning(IOwner owner, long absolutiveTime) {
        timelineForRunning.computeIfAbsent(absolutiveTime, obj -> new ConcurrentHashMap<>()).putIfAbsent(owner, owner);
    }

    /**
     * notice, we do not recommend that add a sending event whose time point is the same with current clock (i.e., the time cost of a sending event is zero.).
     * TODO want to fix it.
     * @param owner
     * @param absolutiveTime
     */
    public void addAbsoluteTimepointForSending(IOwner owner, long absolutiveTime) {
        timelineForSending.computeIfAbsent(absolutiveTime, obj -> new ConcurrentHashMap<>()).putIfAbsent(owner, owner);
    }

    public boolean hasNextRunningTime() {
        return !timelineForRunning.isEmpty();
    }

    public boolean hasNextSendingTime() {
        return !timelineForSending.isEmpty();
    }

    public boolean hasNextTime() {
        return hasNextRunningTime() || hasNextSendingTime();
    }

    /**
     * get next latest event time point and that event. This method will poll the event from the specific queue, and update the global clock to the event time point.
     * @return  eventType(sending or running), timePoint, event owners ( Value in the map, i.e., Object, is meaningless)
     *          null if no events registered.
     */
    public Pair<EventType, Entry<Long, Map<IOwner, Object>>> timeElapse() {
        if (this.hasNextRunningTime() && this.hasNextSendingTime()) {
            long nextRunTime = timelineForRunning.firstKey();
            long nextSendTime = timelineForSending.firstKey();
            logger.debug(() -> String.format("check which event (sending or running) is earlier, sending: %d, running: %d", nextSendTime, nextRunTime));
            if (nextRunTime < nextSendTime) {
                time = nextRunTime;
                return new Pair<>(RUNNING, timelineForRunning.pollFirstEntry());
            }
            else {
                time = nextSendTime;
                return new Pair<>(SENDING, timelineForSending.pollFirstEntry());
            }
        }
        else if (this.hasNextSendingTime()) {
            long nextSendTime = timelineForSending.firstKey();
            logger.debug(() -> String.format("next time is sending event, and the current clock is %d,  next time is %d" , time, nextSendTime));
            time = nextSendTime;
            return new Pair<>(SENDING, timelineForSending.pollFirstEntry());
        }
        else if (this.hasNextRunningTime()) {
            long nextRunTime = timelineForRunning.firstKey();
            logger.debug(() -> String.format("next time is running event, and the current clock is %d,  next time is %d" , time, nextRunTime));
            time = nextRunTime;
            return new Pair<>(RUNNING, timelineForRunning.pollFirstEntry());
        }
        else {// both queues are empty
            logger.debug(() -> "no next time, but nextTime is called");
            return null;
        }
    }

    public void logStatus() {
        StringBuilder sb = new StringBuilder();
        timelineForRunning.forEach((time, owners) -> {
            sb.append("time: " + time + ", running owners: ");
            owners.forEach((owner, obj) -> sb.append(owner + "\t"));
            sb.append("\n");
        });

        timelineForSending.forEach((time, owners) -> {
            sb.append("time: " + time + ", sending owners: ");
            owners.forEach((owner, obj) -> sb.append(owner + "\t"));
            sb.append("\n");
        });
        System.out.println(sb.toString());
    }

}
