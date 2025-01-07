package org.example.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    public static void getDocs() throws IOException {
        try(Socket socket = new Socket(InetAddress.getLocalHost(), 8080)) {
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());

            socket.getOutputStream().write("""
                    GET /the HTTP/1.1
                    Host: 127.0.0.1:8080
                    """.getBytes());
            input.mark(2);
            input.read();
            input.reset();
            System.out.println(new String(input.readNBytes(input.available())));
        }
    }
}
