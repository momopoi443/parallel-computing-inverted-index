package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.threadpool.ThreadPool;
import org.example.threadpool.ThreadPoolImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

@Slf4j
public class InvertedIndexServer {

    private final int port;
    private final InvertedIndex index;
    private final ThreadPool threadPool;

    public InvertedIndexServer(int port, int threadPoolWorkersCount, int queueSize,
                               int bulkDocsUpdateThreadsCount, String documentsDirectory, int updatePeriod) {
        this.port = port;
        index = new InvertedIndex(bulkDocsUpdateThreadsCount, documentsDirectory, updatePeriod);
        threadPool = new ThreadPoolImpl(threadPoolWorkersCount, queueSize);
        threadPool.start();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Started server. Listening on port {}", port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("Client connection from {}:{}", clientSocket.getRemoteSocketAddress(), clientSocket.getPort());

                var submitted = threadPool.submit(() -> {
                    PrintWriter writer;
                    try {
                        writer = new PrintWriter(clientSocket.getOutputStream());
                    } catch (IOException e) {
                        log.error("Error occurred", e);
                        try {
                            clientSocket.close();
                        } catch (IOException ex) {
                            log.error("Error occurred while closing connection", e);
                        }
                        return;
                    }

                    try {
                        handleRequest(clientSocket, writer);
                    } catch (SocketException e) {
                        log.error("Socket exception", e);
                    } catch (Exception e) {
                        log.error("Error occurred", e);
                        writer.write("""
                        HTTP/1.1 500 Internal Server Error\r
                        Content-Type: text/plain\r
                        \r
                        Internal Server Error""");
                        writer.flush();
                    } finally {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            log.error("Error occurred while closing connection", e);
                        }
                    }
                });

                if (!submitted) {
                    PrintWriter writer;
                    try {
                        writer = new PrintWriter(clientSocket.getOutputStream());
                    } catch (IOException e) {
                        log.error("Error occurred", e);
                        clientSocket.close();
                        return;
                    }

                    writer.write("""
                        HTTP/1.1 503 Service Unavailable\r
                        Content-Type: text/plain\r
                        \r
                        Service Unavailable""");
                    writer.flush();
                    clientSocket.close();
                    log.info("Couldn't handle request due to load");
                }
            }
        }
    }

    private void handleRequest(Socket clientSocket, PrintWriter writer) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String requestLine = reader.readLine();

        String requestMethod = requestLine.split(" ")[0];
        switch (requestMethod) {
            case "GET":
                handleGet(requestLine, writer);
                break;
//            case "POST":
//                handlePost(writer);
//                break;
            default:
                handleNotAllowedMethod(writer);
        }
    }

    private void handleGet(String requestLine, PrintWriter writer) {
        var term = requestLine.split(" ")[1].substring(1).toLowerCase();
        var docs = index.getDocuments(term);

        String response = """
           HTTP/1.1 200 OK\r
           Content-Type: text/plain\r
           \r
           """ + docs.toString();
        writer.write(response);
        writer.flush();
    }

    private void handleNotAllowedMethod(PrintWriter writer) {
        String response = """
                HTTP/1.1 405 Method Not Allowed\r
                Content-Type: text/plain\r
                \r
                Method Not Supported
            """;
        writer.write(response);
        writer.flush();
    }

//    private void handlePost(String requestLine, PrintWriter writer) {
//        var docname = requestLine.split(" ")[1].substring(1);
//        index.addDocument();
//    }
}
