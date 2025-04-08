package OS;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicInteger;

// Represents a single fork as a lockable resource
class Fork {
    private final ReentrantLock lock = new ReentrantLock();

    // Attempt to pick up the fork (non-blocking)
    boolean pickUp() {
        return lock.tryLock();
    }

    // Release the fork
    void putDown() {
        lock.unlock();
    }
}

// Represents a philosopher with dynamic priority logic
class Philosopher implements Comparable<Philosopher> {
    private static final AtomicInteger ID_GEN = new AtomicInteger(0);

    int id;
    int hungerLevel;
    long requestTime;
    Fork leftFork;
    Fork rightFork;

    Philosopher(Fork leftFork, Fork rightFork, int hungerLevel) {
        this.id = ID_GEN.getAndIncrement();
        this.hungerLevel = hungerLevel;
        this.leftFork = leftFork;
        this.rightFork = rightFork;
    }

    // Calculates priority score: based on hunger and how long they've waited
    int getPriorityScore() {
        long waitTime = System.currentTimeMillis() - requestTime;
        return (int) waitTime + hungerLevel * 100;
    }

    // Send a request to the scheduler to be allowed to eat
    void requestToEat(Scheduler scheduler) {
        this.requestTime = System.currentTimeMillis();
        scheduler.addToQueue(this);
    }

    // Simulates eating
    void eat() {
        System.out.println("Philosopher " + id + " is eating. Priority: " + getPriorityScore());
        try {
            Thread.sleep(1000); // eating duration
        } catch (InterruptedException ignored) {}
        System.out.println("Philosopher " + id + " finished eating.");
    }

    // Tries to acquire both forks and eat
    boolean tryToEat() {
        if (leftFork.pickUp()) {
            if (rightFork.pickUp()) {
                eat();
                rightFork.putDown();
                leftFork.putDown();
                return true;
            } else {
                leftFork.putDown(); // can't get both, release left
            }
        }
        return false; // couldn't eat
    }

    // Used by priority queue to sort by dynamic priority
    @Override
    public int compareTo(Philosopher other) {
        return Integer.compare(other.getPriorityScore(), this.getPriorityScore());
    }
}

// Central scheduler that manages philosopher requests based on dynamic priorities
class Scheduler {
    private final PriorityBlockingQueue<Philosopher> queue = new PriorityBlockingQueue<>();

    // Add philosopher to queue
    void addToQueue(Philosopher p) {
        queue.add(p);
    }

    // Start serving philosophers based on priority
    void start() {
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    Philosopher p = queue.take(); // highest-priority philosopher
                    if (!p.tryToEat()) {
                        // If eating failed (forks busy), requeue after brief wait
                        Thread.sleep(100);
                        p.requestToEat(this);
                    }
                } catch (InterruptedException ignored) {}
            }
        });
    }
}

// Main class to simulate the philosophers and start the system
public class CaspianDiningPhilosophers {
    public static void main(String[] args) {
        int numPhilosophers = 5;
        Fork[] forks = new Fork[numPhilosophers];

        // Initialize forks
        for (int i = 0; i < numPhilosophers; i++) {
            forks[i] = new Fork();
        }

        Scheduler scheduler = new Scheduler();
        scheduler.start();

        Random rand = new Random();

        // Create and start philosopher threads
        for (int i = 0; i < numPhilosophers; i++) {
            Fork left = forks[i];
            Fork right = forks[(i + 1) % numPhilosophers];
            int hunger = rand.nextInt(10) + 1; // random hunger level

            Philosopher p = new Philosopher(left, right, hunger);

            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(rand.nextInt(3000)); // thinking time
                        p.requestToEat(scheduler); // become hungry and request to eat
                    } catch (InterruptedException ignored) {}
                }
            }).start();
        }
    }
}

