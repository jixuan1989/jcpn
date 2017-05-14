package model.cassandra.fileprocess;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by leven on 2017/1/8.
 */
public class Statistic {

    // rid, node, time.
    static Map<Integer, Map<String, Integer>> ridNodeTimes = new TreeMap<>();

    // rid; coordinator, processor1, processor2, processor3
    static Map<Integer, List<String>> ridNodes = new TreeMap<>();

<<<<<<< HEAD:src/test/java/cn/edu/thu/fileprocess/Statistic.java
    static String x = "0.875";
    static String path = "/Users/leven/Desktop/experiment/res3/" + x + "/";
=======
    //static String x = "0.5";
    static String path = null;
>>>>>>> 18150023ffaf95f9c7d126d8dbdacffa9caea952:models/model/cassandra/fileprocess/Statistic.java

    static void originFileProcessAvg(String fileName) {
        Map<String, Map<String, Integer>> in104 = new HashMap<>();
        Map<String, Map<String, Integer>> in102 = new HashMap<>();
        Map<String, Map<String, Integer>> out102 = new HashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.parallel().forEach(line -> {
                String[] data = line.split(",");

                String rid = data[0];
                int time = Integer.parseInt(data[1]);
                if (data[3].equals("to")) {
                    in104.computeIfAbsent(rid, obj -> new HashMap<>()).put(data[2], time);
                }
                else if (data[3].equals("get")) {
                    in102.computeIfAbsent(rid, obj -> new HashMap<>()).put(data[2], time);
                }
                else {
                    out102.computeIfAbsent(rid, obj -> new HashMap<>()).put(data[2], time);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        int count = 0;
        long sum = 0;
        for (Map.Entry<String, Map<String, Integer>> ridNodeTimes : in104.entrySet()) {
            String rid = ridNodeTimes.getKey();
            Map<String, Integer> nodeTimes = ridNodeTimes.getValue();
            for (Map.Entry<String, Integer> nodeTime : nodeTimes.entrySet()) {
                String node = nodeTime.getKey();
                int start = nodeTime.getValue();
                if (in102.containsKey(rid) && in102.get(rid).containsKey(node)) {
                    int end = in102.get(rid).get(node);
                    int interval = end - start;
                    ++count;
                    sum += interval;
                }
            }
        }
        System.out.println(String.format("sum: %d, count: %d, avg: %f", sum, count, sum / (count + 0.0)));

        count = 0;
        sum = 0;
        for (Map.Entry<String, Map<String, Integer>> ridNodeTimes : in102.entrySet()) {
            String rid = ridNodeTimes.getKey();
            Map<String, Integer> nodeTimes = ridNodeTimes.getValue();
            for (Map.Entry<String, Integer> nodeTime : nodeTimes.entrySet()) {
                String node = nodeTime.getKey();
                int start = nodeTime.getValue();
                if (out102.containsKey(rid) && out102.get(rid).containsKey(node)) {
                    int end = out102.get(rid).get(node);
                    int interval = end - start;
                    ++count;
                    sum += interval;
                }
            }
        }
        System.out.println(String.format("sum: %d, count: %d, avg: %f", sum, count, sum / (count + 0.0)));

    }

    public static void main(String[] args) throws IOException {
        path=args[0];
        String fileName = path + "test.log";
        //originFileProcess(fileName);
        classifyProcess(fileName);
        processMap();
//        originFileProcessAvg(fileName);
    }

    static void classifyProcess(String fileName) {
        // rid, node, finishTime.
        ridNodeTimes = new TreeMap<>();
        ridNodes = new TreeMap<>();

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            int runtime = 0;
            stream.forEach(line -> {
                String[] data = line.split(",");

                if (null == data || data.length <= 2) {
                    ;
                }
                else if (data.length == 3) {
                    int rid = Integer.parseInt(data[0]);
                    String node = data[1];
                    int time = Integer.parseInt(data[2]);
                    ridNodeTimes.computeIfAbsent(rid, obj -> new HashMap<>()).put(node, time);
                }
                else {
                    int rid = Integer.parseInt(data[0]);
                    String coordinator = data[1];
                    String processor1 = data[2];
                    String processor2 = data[3];
                    String processor3 = data[4];

                    List<String> nodes = ridNodes.computeIfAbsent(rid, obj -> new ArrayList<>());
                    nodes.add(coordinator);
                    nodes.add(processor1);
                    nodes.add(processor2);
                    nodes.add(processor3);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void processMap() throws IOException {
        List<Integer> masterSlaves = new ArrayList<>();
        List<Integer> slaves = new ArrayList<>();
        ridNodes.forEach((rid, nodes) -> {
            String coordinator = nodes.get(0);
            if (ridNodeTimes.get(rid).containsKey(coordinator)) {
                int time = ridNodeTimes.get(rid).get(coordinator);

                for (int i = 1; i < nodes.size(); ++i) {
                    String processor = nodes.get(i);
                    if (processor.equals(coordinator)) continue;

                    int processTime = ridNodeTimes.get(rid).get(processor);
                    masterSlaves.add(Math.abs(processTime - time));
                }
            }

            for (int i = 1; i < nodes.size() - 1; ++i) {
                String processor1 = nodes.get(i);
                int processTime1 = ridNodeTimes.get(rid).get(processor1);

                for (int j = i + 1; j < nodes.size(); ++j) {
                    String processor2 = nodes.get(j);
                    int processTime2 = ridNodeTimes.get(rid).get(processor2);
                    slaves.add(Math.abs(processTime1 - processTime2));
                }
            }
        });

        masterSlaves.sort((a, b) -> a.compareTo(b));
        String fileName1 = path + "co2pro.csv";
        write(fileName1, masterSlaves);

        slaves.sort((a, b) -> a.compareTo(b));
        String fileName2 = path + "pro2pro.csv";
        write(fileName2, slaves);
    }

    private static void write(String fileName, List<Integer> res) throws IOException {
        Path path = Paths.get(fileName);
        BufferedWriter writer = Files.newBufferedWriter(path);
        for (Integer time : res) {
            try {
                writer.write(time.toString());
                writer.newLine();
            }
            catch (Exception ex) {
                System.out.println(time);
            }
        }
        writer.flush();
        writer.close();
    }

    static void originFileProcess(String fileName) {
        Map<String, Integer> coordinators = new HashMap<>();
        Map<String, Integer> processors = new HashMap<>();
        Map<String, Integer> hashCodes = new HashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.parallel().forEach(line -> {
                String[] data = line.split(",");
                coordinators.compute(data[0], (key, value) -> (value == null) ? 1 : value + 1);

                processors.compute(data[1], (key, value) -> (value == null) ? 1 : value + 1);
                processors.compute(data[2], (key, value) -> (value == null) ? 1 : value + 1);
                processors.compute(data[3], (key, value) -> (value == null) ? 1 : value + 1);

                hashCodes.compute(data[4], (key, value) -> (value == null) ? 1 : value + 1);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
