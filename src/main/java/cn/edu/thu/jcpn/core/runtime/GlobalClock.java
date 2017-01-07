package cn.edu.thu.jcpn.core.runtime;

import cn.edu.thu.jcpn.common.Triple;
import cn.edu.thu.jcpn.core.runtime.tokens.INode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType.LOCAL;
import static cn.edu.thu.jcpn.core.runtime.GlobalClock.EventType.REMOTE;

public class GlobalClock {

    private static Logger logger = LogManager.getLogger();

    private static GlobalClock globalClock = new GlobalClock();
    private long time = 0L;

    public enum EventType {
        LOCAL, REMOTE
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

    // <time, node, redundant>, redundant value is useless, because java skipListMap doesn't have a concurrent HashSet implementation.
    private ConcurrentSkipListMap<Long, Set<INode>> timelineForLocalEvents = new ConcurrentSkipListMap<>();
    private ConcurrentSkipListMap<Long, Set<INode>> timelineForRemoteEvents = new ConcurrentSkipListMap<>();

    public void addAbsoluteTimePointForLocalHandle(INode node, long absolutiveTime) {
        timelineForLocalEvents.computeIfAbsent(absolutiveTime, obj -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(node);
    }

    public void addAbsoluteTimePointForRemoteHandle(INode node, long absolutiveTime) {
        timelineForRemoteEvents.computeIfAbsent(absolutiveTime, obj -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(node);
    }

    public boolean hasNextRunningTime() {
        return !timelineForLocalEvents.isEmpty();
    }

    public boolean hasNextSendingTime() {
        return !timelineForRemoteEvents.isEmpty();
    }

    public boolean hasNextTime() {
        return hasNextRunningTime() || hasNextSendingTime();
    }

    /**
     * get next latest event time point and that event. This method will poll the event from the specific queue, and update the global clock to the event time point.
     *
     * @return eventType(sending or running), timePoint, event owners ( Value in the map, i.e., Object, is meaningless)
     * null if no events registered.
     */
    public Triple<EventType, Long, Set<INode>> timeElapse() {
        if (!hasNextTime()) {
            logger.debug(() -> "no next time, but nextTime is called");
            return null;
        }

        long nextRemoteTime = timelineForRemoteEvents.isEmpty() ? Long.MAX_VALUE : timelineForRemoteEvents.firstKey();
        long nextLocalTime = timelineForLocalEvents.isEmpty() ? Long.MAX_VALUE : timelineForLocalEvents.firstKey();
        EventType eventType = (nextLocalTime < nextRemoteTime) ? LOCAL : REMOTE;
        Entry<Long, Set<INode>> timeNodes = (eventType.equals(LOCAL)) ? timelineForLocalEvents.pollFirstEntry() : timelineForRemoteEvents.pollFirstEntry();

        logger.debug(() -> String.format("next time is %s event, and the current clock is %d, next time is %d", eventType.toString(), time, timeNodes.getKey()));
        time = timeNodes.getKey();

        return new Triple<>(eventType, timeNodes.getKey(), timeNodes.getValue());
    }

    public void logStatus() {
        StringBuilder sb = new StringBuilder();
        timelineForLocalEvents.forEach((time, nodes) -> {
            sb.append("at time " + time + ", will handle local event on nodes: ");
            nodes.forEach(node -> sb.append(node + "\t"));
            sb.append("\n");
        });

        timelineForRemoteEvents.forEach((time, nodes) -> {
            sb.append("at time " + time + ", will handle remote event on nodes: ");
            nodes.forEach(node -> sb.append(node + "\t"));
            sb.append("\n");
        });
        System.out.println(sb.toString());
    }

}
