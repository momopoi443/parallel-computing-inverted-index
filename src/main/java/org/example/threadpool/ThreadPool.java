package org.example.threadpool;

public interface ThreadPool {

    //додає задачу до чегри
    boolean submit(Runnable task);

    //створює потоки і стартує їх
    void start();

    //знімає з паузи потоки
    void resume();

    //потоки дороблюють їх поточну задачу і далі встають в стан паузи
    void pause();

    //потоки дороблюють їх поточну задачу і зашершуються
    void stop();
}
