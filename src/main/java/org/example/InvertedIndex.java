package org.example;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class InvertedIndex {

    public final Path DOCUMENTS_DIRECTORY = Path.of("C:\\Users\\alex\\Desktop\\docs");
    public final ConcurrentMap<String, List<String>> index = new ConcurrentHashMap<>();

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
    public InvertedIndex(int threadCount, List<String> documents) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new IndexWorker(i, threadCount, documents);
            thread.start();
            threads.add(thread);
        }

        for (var thread : threads) {thread.join();}
    }

    public List<String> getDocuments(String key) {
        return index.get(key);
    }

    public void addDocument(String document) {
        synchronized (index) {
            indexDocument(document);
        }
    }

    @SneakyThrows
    private void indexDocument(String document) {
        String content = Files.readString(DOCUMENTS_DIRECTORY.resolve(document));
        String[] words = content.replaceAll("[^A-Za-z\\s]", "").toLowerCase().split("\\s+");
        Set<String> terms = new HashSet<>(Arrays.asList(words));

        for (String term : terms) {
            index.merge(term, new ArrayList<>(List.of(document)), (oldVal, newVal) -> {
                oldVal.add(document);
                return oldVal;
            });
        }

        InvertedIndex.log.info("indexed document: {}", document);
    }
}
