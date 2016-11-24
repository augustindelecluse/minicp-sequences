package minicp.examples.ptp;

import minicp.util.Procedure;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class PTPBenchmark {

    private String solResumeFile = "data/ptp/solutions/solution.txt";
    private final int stateOfTheArtMaxTime = 1800; // 1800s timeout

    public InstanceFile[] stateOfTheArtInstanceFiles = new InstanceFile[]{
            new InstanceFile("data/ptp/easy/PTP-RAND-1_4_2_16.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/easy/PTP-RAND-1_8_4_32.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/easy/PTP-RAND-1_12_5_48.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/easy/PTP-RAND-1_16_6_64.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/easy/PTP-RAND-1_20_8_80.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/easy/PTP-RAND-1_24_9_96.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/easy/PTP-RAND-1_28_10_112.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/easy/PTP-RAND-1_32_12_128.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/easy/PTP-RAND-1_36_14_144.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/easy/PTP-RAND-1_40_16_160.json", stateOfTheArtMaxTime),

            new InstanceFile("data/ptp/medium/PTP-RAND-1_8_2_16.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/medium/PTP-RAND-1_16_3_32.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/medium/PTP-RAND-1_24_4_48.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/medium/PTP-RAND-1_32_4_64.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/medium/PTP-RAND-1_40_5_80.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/medium/PTP-RAND-1_48_5_96.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/medium/PTP-RAND-1_56_6_112.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/medium/PTP-RAND-1_64_8_128.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/medium/PTP-RAND-1_72_8_144.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/medium/PTP-RAND-1_80_9_160.json", stateOfTheArtMaxTime),

            new InstanceFile("data/ptp/hard/PTP-RAND-1_16_2_16.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/hard/PTP-RAND-1_32_3_32.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/hard/PTP-RAND-1_48_4_48.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/hard/PTP-RAND-1_64_4_64.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/hard/PTP-RAND-1_80_5_80.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/hard/PTP-RAND-1_96_5_96.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/hard/PTP-RAND-1_112_6_112.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/hard/PTP-RAND-1_128_8_128.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/hard/PTP-RAND-1_144_8_144.json", stateOfTheArtMaxTime),
            new InstanceFile("data/ptp/hard/PTP-RAND-1_160_8_160.json", stateOfTheArtMaxTime),
    };

    public record InstanceFile(String instancePath, int maxRunTime) {

    }

    private final Semaphore writerSemaphore = new Semaphore(1);

    private void WithWriterSemaphore(Procedure procedure) {
        try {
            writerSemaphore.acquire();
            procedure.call();
            writerSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(String filePath, String detail) {
        writeToFile(filePath, detail, true);
    }

    private void writeToFile(String filePath, String detail, boolean append) {
        try {
            FileWriter writer = new FileWriter(filePath, append);
            writer.write(detail);
            writer.close();
        } catch (IOException e) {
            System.out.println("error when writing to file " + filePath);
        }
    }

    public PTPBenchmark() {

    }

    public void runFile(InstanceFile instanceFile) {
        PTPInstance instance = new PTPInstance(instanceFile.instancePath);
        PTPInsertion solver = new PTPInsertion(instance);
        solver.solve(instanceFile.maxRunTime);
        System.out.println(instanceFile.instancePath + " : nServiced = " + solver.getNBestPatientServiced());
        WithWriterSemaphore(() -> {
            writeToFile(solResumeFile, instanceFile.instancePath + " : nServiced = " +
                    solver.getNBestPatientServiced() + '\n');
        });
    }

    public void solve() {
        // reset the solution file
        writeToFile(solResumeFile, "", false);
        int maxParallel = Runtime.getRuntime().availableProcessors() - 1;
        int maxListSize = 2 * maxParallel;
        ArrayList<InstanceFile> fileArrayList = new ArrayList<>();
        for (InstanceFile instanceFile: stateOfTheArtInstanceFiles) {
            fileArrayList.add(instanceFile);
            if (fileArrayList.size() == maxListSize) {
                System.out.println("running " + fileArrayList.size() + " experiments");
                fileArrayList.stream().parallel().forEach(this::runFile);
                fileArrayList.clear();
            }
        }
        System.out.println("running " + fileArrayList.size() + " experiments");
        fileArrayList.stream().parallel().forEach(this::runFile);
    }

    public static void main(String[] args) {
        PTPBenchmark benchmark = new PTPBenchmark();
        benchmark.solve();
    }

}
