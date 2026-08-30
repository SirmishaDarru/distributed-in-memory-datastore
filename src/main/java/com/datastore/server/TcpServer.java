package com.datastore.server;

import com.datastore.command.CommandRouter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Multithreaded TCP server: accept loop on the caller thread, per-connection work on a
 * bounded {@link ExecutorService} thread pool.
 */
public class TcpServer {

    private static final int THREAD_POOL_SIZE = 10;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final int port;
    private final ExecutorService workerPool;
    private final CommandRouter commandRouter;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public TcpServer(int port) {
        this.port = port;
        this.workerPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.commandRouter = new CommandRouter();
    }

    /**
     * Binds the server socket and blocks in an accept loop until {@link #stop()} is called
     * or the socket is closed.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));
        running = true;

        System.out.println("TCP server listening on port " + port
                + " (worker pool size " + THREAD_POOL_SIZE + ")");

        while (running) {
            try {
                Socket client = serverSocket.accept();
                System.out.println("Accepted connection from " + client.getRemoteSocketAddress());
                workerPool.submit(new ClientHandler(client, commandRouter));
            } catch (SocketException e) {
                if (!running) {
                    break;
                }
                System.err.println("Server socket error: " + e.getMessage());
            } catch (IOException e) {
                if (!running) {
                    break;
                }
                System.err.println("Failed to accept connection: " + e.getMessage());
            }
        }
    }

    /**
     * Stops the accept loop, closes the server socket, and shuts down the worker pool.
     * Safe to call from a JVM shutdown hook.
     */
    public void stop() {
        running = false;

        ServerSocket localServer = serverSocket;
        if (localServer != null && !localServer.isClosed()) {
            try {
                localServer.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }

        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("TCP server stopped");
    }
}
