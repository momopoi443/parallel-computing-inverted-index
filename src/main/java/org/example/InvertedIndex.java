package org.example;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

@Slf4j
public class InvertedIndex {

    private final int threadCount;
    private final Path documentDir;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentMap<String, List<String>> index = new ConcurrentHashMap<>();
    private final Map<String, Object> indexedDocs = new HashMap<>();

    @AllArgsConstructor
    private class IndexWorker extends Thread {
        private final int threadNum;
        private final int offset;
        private final List<String> documents;

        @Override
        @SneakyThrows
        public void run() {
            for (int i = threadNum; i < documents.size(); i += offset) {
                String document = documents.get(i);
                indexDocument(document);
            }
        }
    }

    @SneakyThrows
    public InvertedIndex(int threadCount, String documentDir, int updatePeriod) {
        this.documentDir = Path.of(documentDir);
        this.threadCount = threadCount;
        updateIndex();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                updateIndex();
            }
        }, updatePeriod, updatePeriod);
    }

    public List<String> getDocuments(String key) {
        try {
            lock.readLock().lock();
            return index.getOrDefault(key, List.of());
        } finally {
            lock.readLock().unlock();
        }
    }

//    public void addDocument(String document) {
//        synchronized (index) {
//            indexDocument(document);
//        }
//    }

    @SneakyThrows
    private void indexDocument(String document) {
        String content = Files.readString(documentDir.resolve(document));
        String[] words = content.replaceAll("[^A-Za-z\\s]", "").toLowerCase().split("\\s+");
        Set<String> terms = new HashSet<>(Arrays.asList(words));

        for (String term : terms) {
            index.merge(term, new ArrayList<>(List.of(document)), (oldVal, newVal) -> {
                oldVal.add(document);
                return oldVal;
            });
        }

        indexedDocs.put(document, new Object());
        InvertedIndex.log.info("indexed document: {}", document);
    }

    private void updateIndex() {
        List<String> newDocs = Stream.of(documentDir.toFile().listFiles())
                .map(File::getName)
                .filter(d -> !indexedDocs.containsKey(d))
                .toList();

        try {
            lock.writeLock().lock();
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Thread thread = new IndexWorker(i, threadCount, newDocs);
                thread.start();
                threads.add(thread);
            }

            for (var thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException ignored) {}
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
