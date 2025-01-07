package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        InvertedIndexServer server = new InvertedIndexServer(
                Integer.decode(System.getenv("APP_PORT")),
                Integer.decode(System.getenv("SERVER_WORKERS")),
                Integer.decode(System.getenv("SERVER_TASK_QUEUE_SIZE")),
                Integer.decode(System.getenv("INDEX_THREADS")),
                System.getenv("DOCUMENTS_DIRECTORY"),
                Integer.decode(System.getenv("DIRECTORY_SCAN_PERIOD"))
        );
        server.start();
    }
}