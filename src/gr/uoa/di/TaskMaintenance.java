package gr.uoa.di;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class TaskMaintenance {

    private ScheduledExecutorService taskCleaner;
    private final ArrayList<Thread> taskThreadList;


    TaskMaintenance() {
        taskThreadList = new ArrayList<>(1000);
        taskCleaner = Executors.newSingleThreadScheduledExecutor();
        taskCleaner.scheduleAtFixedRate(this::cleanup, 30, 60, TimeUnit.SECONDS);
    }

    private synchronized void cleanup() {
        System.out.println("Cleanup for " + taskThreadList.size() + " threads");
        while (taskThreadList.size() > 0) {
            Thread taskThread = taskThreadList.get(0);
            try {
                taskThread.join( 1000);
                removeTaskThread(taskThread);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        printArrayList();
        System.out.println("Cleanup ended");
    }

    synchronized void addTaskThread(Thread taskThread) {
        taskThreadList.add(taskThread);
    }

    private synchronized void removeTaskThread(Thread taskThread) {
        taskThreadList.remove(taskThread);
    }

    synchronized void shutDown(String node) {
        System.out.println("Initiating shutdown of node " + node);
        printArrayList();
        removeTaskThread(Thread.currentThread());
        taskCleaner.shutdownNow();
        cleanup();
        assert taskThreadList.size() == 0: "Task arraylist has more than one tasks";
        System.out.println("Shutdown of Task Maintenance done of node " + node);
    }

    private synchronized void printArrayList() {
        int i = 0;
        System.out.println("Current thread id = " + Thread.currentThread().getId());
        for (Thread t: taskThreadList) {
            System.out.println(i + " = " + t.getId());
            i++;
        }
    }

}
