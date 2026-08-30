package com.datastore;

import com.datastore.server.TcpServer;

import java.io.IOException;

/**
 * Application entry point. Starts the Phase 1 TCP server on the Redis default port.
 */
public final class Main {

    private static final int DEFAULT_PORT = 6379;

    private Main() {
    }

    public static void main(String[] args) {
        TcpServer server = new TcpServer(DEFAULT_PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook: stopping server...");
            server.stop();
        }, "datastore-shutdown"));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server on port " + DEFAULT_PORT + ": " + e.getMessage());
            server.stop();
            System.exit(1);
        }
    }
}
