package org.example.threadpool;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ThreadPoolImpl implements ThreadPool {

    private final int workersCount;

    private final List<Worker> workers;

    private final int taskQueueSize;

    private final Queue<Runnable> taskQueue;

    private final ThreadPoolStatus status;

    public ThreadPoolImpl(int workersCount, int taskQueueSize) {
        this.workersCount = workersCount;
        workers = new ArrayList<>();
        this.taskQueueSize = taskQueueSize;
        taskQueue = new LinkedList<>();
        status = new ThreadPoolStatus(ThreadPoolStatusEnum.CREATED);
    }

    enum ThreadPoolStatusEnum {
        CREATED, STARTED, PAUSED, STOPPED
    }


    @Setter
    @Getter
    static class ThreadPoolStatus {

        private ThreadPoolStatusEnum statusEnum;

        public ThreadPoolStatus(ThreadPoolStatusEnum statusEnum) {
            this.statusEnum = statusEnum;
        }

    }

    class Worker extends Thread {

        private boolean isRunning = true;

        private boolean isPaused = false;

        private final Object pauseLock = new Object();

        @Override
        public void run() {
            while (isRunning) {
                synchronized (pauseLock) {
                    if (isPaused) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException ignored) {}
                    }
                }
                if (!isRunning) break;
                synchronized (taskQueue) {
                    if (taskQueue.isEmpty()) {
                        try {
                            taskQueue.wait();
                        } catch (InterruptedException ignored) {}
                    }
                }
                synchronized (pauseLock) {
                    if (isPaused) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException ignored) {}
                    }
                }
                if (!isRunning) break;

                try {
                    taskQueue.poll().run();
                    log.info("Task done");
                } catch (Exception ignored) {}
            }
        }

        public void stopWorker() {
            synchronized (pauseLock) {
                if (!isRunning) {
                    throw new IllegalStateException("thread is not running");
                }

                isRunning = false;
                isPaused = false;
                pauseLock.notify();
            }
        }

        public synchronized void pauseWorker() {
            if (!isRunning) {
                throw new IllegalStateException("thread is not running");
            }
            isPaused = true;
        }

        public void resumeWorker() {
            synchronized (pauseLock) {
                if (!isRunning || !isPaused) {
                    throw new IllegalStateException("thread is not running or already paused");
                }

                isPaused = false;
                pauseLock.notify();
            }
        }
    }

    @Override
    public boolean submit(Runnable task) {
        synchronized (status) {
            if (status.getStatusEnum() == ThreadPoolStatusEnum.STOPPED) {
                throw new IllegalStateException("Can't add task if thread pool stopped");
            }

            synchronized (taskQueue) {
                if (taskQueue.size() + 1 > taskQueueSize) {
                    return false;
                }

                taskQueue.add(task);
                taskQueue.notify();
                return true;
            }
        }
    }

    @Override
    public void start() {
        synchronized (status) {
            var statusEnum = status.getStatusEnum();
            if (statusEnum == ThreadPoolStatusEnum.PAUSED || statusEnum == ThreadPoolStatusEnum.STARTED) {
                throw new IllegalStateException("Thread pool already started");
            }

            for (int i = 0; i < workersCount; i++) {
                var worker = new Worker();
                workers.add(worker);
                worker.start();
            }
            status.setStatusEnum(ThreadPoolStatusEnum.STARTED);
        }
    }

    @Override
    public void resume() {
        synchronized (status) {
            if (status.getStatusEnum() != ThreadPoolStatusEnum.PAUSED) {
                throw new IllegalStateException("Thread pool thread must be paused");
            }

            workers.forEach(Worker::resumeWorker);
            status.setStatusEnum(ThreadPoolStatusEnum.STARTED);
        }
    }

    @Override
    public void pause() {
        synchronized (status) {
            if (status.getStatusEnum() != ThreadPoolStatusEnum.STARTED) {
                throw new IllegalStateException("Thread pool thread must be stared");
            }

            synchronized (taskQueue) {
                workers.forEach(Worker::pauseWorker);
                taskQueue.notifyAll();
                status.setStatusEnum(ThreadPoolStatusEnum.PAUSED);
            }
        }
    }

    @Override
    public void stop() {
        synchronized (status) {
            var statusEnum = status.getStatusEnum();
            if (statusEnum == ThreadPoolStatusEnum.CREATED || statusEnum == ThreadPoolStatusEnum.STOPPED) {
                throw new IllegalStateException("Thread pool already stopped or only created");
            }

            synchronized (taskQueue) {
                workers.forEach(Worker::stopWorker);
                taskQueue.notifyAll();
                status.setStatusEnum(ThreadPoolStatusEnum.STOPPED);
            }
        }
    }
}
