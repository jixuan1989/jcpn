package cn.edu.thu.fileprocess;

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

    static void originFileProcessAvg(String fileName) {
        Map<String, Integer> in104 = new HashMap<>();
        Map<String, Integer> in102 = new HashMap<>();
        Map<String, Integer> out102 = new HashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.parallel().forEach(line -> {
                String[] data = line.split(",");

                String rid = data[0];
                int time = Integer.parseInt(data[1]);
                if (data[2].equals("in")) {
                    if (in104.containsKey(rid)) {
                        int oldTime = in104.get(rid);
                        in104.put(rid, Math.min(oldTime, time));
                        in102.put(rid, Math.max(oldTime, time));
                    }
                    else in104.put(rid, time);
                }
                else {
                    out102.put(rid, time);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        int count = 0;
        long sum = 0;
        for (Map.Entry<String, Integer> entry : in104.entrySet()) {
            String rid = entry.getKey();
            int start = entry.getValue();
            if (in102.containsKey(rid)) {
                int end = in102.get(rid);
                int interval = end - start;
                ++count;
                sum += interval;
            }
        }
        System.out.println(sum / (count + 0.0));

        count = 0;
        sum = 0;
        for (Map.Entry<String, Integer> entry : in102.entrySet()) {
            String rid = entry.getKey();
            int start = entry.getValue();
            if (out102.containsKey(rid)) {
                int end = out102.get(rid);
                int interval = end - start;
                ++count;
                sum += interval;
            }
        }
        System.out.println(sum / (count + 0.0));
    }

    public static void main(String[] args) throws IOException {
        String fileName = "src/test/resources/output/test.log";
        //originFileProcess(fileName);
        //classifyProcess(fileName);
        //processMap();
        originFileProcessAvg(fileName);
    }

    static void classifyProcess(String fileName) {
        // rid, node, finishTime.
        ridNodeTimes = new HashMap<>();
        ridNodes = new HashMap<>();

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(line -> {
                String[] data = line.split(",");

                if (data.length == 3) {
                    int rid = Integer.parseInt(data[2]);
                    String node = data[1];
                    int time = Integer.parseInt(data[0]);
                    ridNodeTimes.computeIfAbsent(rid, obj -> new HashMap<>()).put(node, time);
                }
                else {
                    String coordinator = data[0];
                    String processor1 = data[1];
                    String processor2 = data[2];
                    String processor3 = data[3];
                    int rid = Integer.parseInt(data[4]);

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

        String fileName1 = "src/test/resources/process/co2pro.csv";
        write(fileName1, masterSlaves);

        String fileName2 = "src/test/resources/process/pro2pro.csv";
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
