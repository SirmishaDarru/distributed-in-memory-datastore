package com.datastore.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles a single client TCP connection on a worker thread from {@link TcpServer}'s pool.
 *
 * <p>Phase 1 does not parse RESP yet. Incoming bytes are logged and a dummy simple-string
 * reply {@code +OK\r\n} is written so clients (including {@code redis-cli}) receive a
 * well-formed RESP response.
 */
public class ClientHandler implements Runnable {

    private static final int BUFFER_SIZE = 4096;
    private static final byte[] DUMMY_OK_RESPONSE = "+OK\r\n".getBytes(StandardCharsets.UTF_8);

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress() != null
                ? socket.getRemoteSocketAddress().toString()
                : "unknown";

        try (Socket clientSocket = socket;
             InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                String received = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                System.out.println("[client " + remote + "] " + received);

                out.write(DUMMY_OK_RESPONSE);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("I/O error on connection " + remote + ": " + e.getMessage());
        } finally {
            System.out.println("Client disconnected: " + remote);
        }
    }
}
