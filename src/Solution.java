import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/*
 * Each Process is a THREAD.
 * HOWEVER: we are NOT doing real parallel execution.
 * The scheduler controls which process "runs" using wait/notify.
 */
class Process extends Thread {

    int id;
    int arrivalTime;
    int remainingTime;

    int waitingTime = 0;
    boolean started = false;

    // Used to control execution
    boolean canRun = false;
    boolean finished = false;

    final Object lock; // shared lock with scheduler

    public Process(int id, int arrival, int burst, Object lock) {
        this.id = id;
        this.arrivalTime = arrival;
        this.remainingTime = burst;
        this.lock = lock;
    }

    @Override
    public void run() {
        while (!finished) {
            synchronized (lock) {
                try {
                    // Wait until scheduler allows this process to run
                    while (!canRun) {
                        lock.wait();
                    }

                    // Immediately give control back (scheduler simulates time)
                    canRun = false;

                    // Notify scheduler that this process "ran"
                    lock.notifyAll();

                } catch (InterruptedException e) {
                    System.out.println("Process " + id + " interrupted: " + e.getMessage());
                }
            }
        }
    }
}

public class Solution {

    public static void main(String[] args) {

        File filename = new File("input.txt");

        ArrayList<Integer> arrival_time = new ArrayList<>();
        ArrayList<Integer> burst_time = new ArrayList<>();

        // Read input file
        try (Scanner scanner = new Scanner(filename)) {
            while (scanner.hasNextInt()) {
                arrival_time.add(scanner.nextInt());
                burst_time.add(scanner.nextInt());
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
            return;
        }

        // Open output file to write results
        try {
            System.setOut(new java.io.PrintStream("output.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.getMessage());
        }

        // Shared lock for synchronization
        Object lock = new Object();

        // Create process list
        ArrayList<Process> processes = new ArrayList<>();
        for (int i = 0; i < arrival_time.size(); i++) {
            processes.add(new Process(i + 1, arrival_time.get(i), burst_time.get(i), lock));
        }

        // Start all process threads
        for (Process p : processes) {
            p.start();
        }

        int currentTime = 0;
        ArrayList<Process> readyQueue = new ArrayList<>();

        // MAIN SCHEDULER LOOP
        while (true) {

            // Add arriving processes (no duplicates, no finished)
            for (Process p : processes) {
                if (p.arrivalTime == currentTime && p.remainingTime > 0 && !readyQueue.contains(p)) {
                    readyQueue.add(p);
                }
            }

            // Check if all processes are done
            boolean allDone = true;
            for (Process p : processes) {
                if (p.remainingTime > 0) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) break;

            // Sort by shortest remaining time (SRTF)
            readyQueue.sort((a, b) -> {
                if (a.remainingTime != b.remainingTime)
                    return a.remainingTime - b.remainingTime;
                return a.arrivalTime - b.arrivalTime;
            });

            // Remove any finished processes just in case
            while (!readyQueue.isEmpty() && readyQueue.get(0).remainingTime <= 0) {
                readyQueue.remove(0);
            }

            // If nothing is ready → idle
            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }

            Process p = readyQueue.get(0);

            // Quantum = 10% of remaining time (minimum 1)
            int quantum = Math.max(1, p.remainingTime / 10);

            // First time execution
            if (!p.started) {
                System.out.println("Time " + currentTime + ", Process " + p.id + ", Started");
                p.started = true;
            }

            System.out.println("Time " + currentTime + ", Process " + p.id + ", Resumed");

            // Allow this process to run
            synchronized (lock) {
                p.canRun = true;
                lock.notifyAll();

                try {
                    lock.wait(); // wait until process signals back
                } catch (InterruptedException e) {
                    System.out.println("Process " + p.id + " interrupted: " + e.getMessage());
                }
            }

            // Actual execution time (cannot exceed remaining)
            int execTime = Math.min(quantum, p.remainingTime);

            // Simulate execution unit by unit
            for (int t = 0; t < execTime; t++) {

                currentTime++;
                
             // Update waiting time
                // ONLY count if process has already started (important fix)
                for (Process other : readyQueue) {
                	if (other != p && other.remainingTime > 0) {
                        other.waitingTime++;
                    }
                }

                // Add new arrivals DURING execution
                for (Process newP : processes) {
                    if (newP.arrivalTime == currentTime && newP.remainingTime > 0 && !readyQueue.contains(newP)) {
                        readyQueue.add(newP);
                    }
                }

                
            }

            // Decrease remaining time
            p.remainingTime -= execTime;

            // If finished
            if (p.remainingTime == 0) {
                System.out.println("Time " + currentTime + ", Process " + p.id + ", Finished");

                readyQueue.remove(p);
                p.remainingTime = -1;

                // Stop thread
                p.finished = true;

            } else {
                // Otherwise pause and move to back (round-robin behavior)
                System.out.println("Time " + currentTime + ", Process " + p.id + ", Paused");

                readyQueue.remove(p);
                readyQueue.add(p);
            }
        }

        // Print results
        System.out.println("-------------------------");
        System.out.println("Waiting Times:");

        for (Process p : processes) {
            System.out.println("Process " + p.id + ": " + p.waitingTime);
        }
    }
}