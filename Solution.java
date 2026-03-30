import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

class Process {
    int id;
    int arrivalTime;
    int burstTime;
    int remainingTime;

    int waitingTime = 0;
    boolean started = false;

    public Process(int id, int arrival, int burst) {
        this.id = id;
        this.arrivalTime = arrival;
        this.burstTime = burst;
        this.remainingTime = burst;
    }
}

public class Solution {

    public static void main(String[] args) {

        File filename = new File("input.txt");

        ArrayList<Integer> arrival_time = new ArrayList<>();
        ArrayList<Integer> burst_time = new ArrayList<>();

        try (Scanner scanner = new Scanner(filename)) {
            while (scanner.hasNextInt()) {
                int arrival = scanner.nextInt();
                int burst = scanner.nextInt();

                arrival_time.add(arrival);
                burst_time.add(burst);
            }

        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
            return;
        }

        ArrayList<Process> processes = new ArrayList<>();

        for (int i = 0; i < arrival_time.size(); i++) {
            processes.add(new Process(i + 1, arrival_time.get(i), burst_time.get(i)));
        }

        int currentTime = 0;
        ArrayList<Process> readyQueue = new ArrayList<>();

        while (true) {

            // Add arrivals (FIX: prevent re-adding finished processes)
            for (Process p : processes) {
                if (p.arrivalTime == currentTime && p.remainingTime > 0 && !readyQueue.contains(p)) {
                    readyQueue.add(p);
                }
            }

            // Check if all done
            boolean allDone = true;
            for (Process p : processes) {
                if (p.remainingTime > 0) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) break;

            // Sort SRTF = "shortest remaining time first"
            for (int i = 0; i < readyQueue.size() - 1; i++) {
                int minIndex = i;

                for (int j = i + 1; j < readyQueue.size(); j++) {
                    Process a = readyQueue.get(j);
                    Process b = readyQueue.get(minIndex);

                    if (a.remainingTime < b.remainingTime ||
                        (a.remainingTime == b.remainingTime && a.arrivalTime < b.arrivalTime)) {
                        minIndex = j;
                    }
                }

                Process temp = readyQueue.get(i);
                readyQueue.set(i, readyQueue.get(minIndex));
                readyQueue.set(minIndex, temp);
            }

            // remove any finished processes before selecting
            while (!readyQueue.isEmpty() && readyQueue.get(0).remainingTime <= 0) {
                readyQueue.remove(0);
            }

            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }

            Process p = readyQueue.get(0);

            int quantum = Math.max(1, p.remainingTime / 10); // execution quantum

            if (!p.started) {
                System.out.println("Time " + currentTime + ", Process " + p.id + ", Started");
                p.started = true;
            }

            System.out.println("Time " + currentTime + ", Process " + p.id + ", Resumed");

            int execTime = Math.min(quantum, p.remainingTime);

            for (int t = 0; t < execTime; t++) {

                currentTime++;

                // Add arrivals during execution 
                for (Process newP : processes) {
                    if (newP.arrivalTime == currentTime && newP.remainingTime > 0) {
                        readyQueue.add(newP);
                    }
                }

                // Waiting time 
                for (Process other : readyQueue) {
                    if (other != p && other.remainingTime > 0) {
                        other.waitingTime++;
                    }
                }
            }

            p.remainingTime -= execTime;

            // Check if finished or paused
            if (p.remainingTime == 0) {
                System.out.println("Time " + currentTime + ", Process " + p.id + ", Finished");

                readyQueue.remove(p);
                p.remainingTime = -1;

            } else {
                System.out.println("Time " + currentTime + ", Process " + p.id + ", Paused");

                readyQueue.remove(p);
                readyQueue.add(p);
            }
        }

        System.out.println("-------------------------");
        System.out.println("Waiting Times:");

        for (Process p : processes) {
            System.out.println("Process " + p.id + ": " + p.waitingTime);
        }
    }
}