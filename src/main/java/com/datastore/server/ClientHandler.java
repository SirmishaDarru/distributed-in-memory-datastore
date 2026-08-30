package com.datastore.server;

import com.datastore.command.CommandRouter;
import com.datastore.protocol.RespParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

/**
 * Handles a single client TCP connection: parse RESP, route the command, write the reply.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final CommandRouter commandRouter;
    private final RespParser respParser;

    public ClientHandler(Socket socket, CommandRouter commandRouter) {
        this.socket = socket;
        this.commandRouter = commandRouter;
        this.respParser = new RespParser();
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress() != null
                ? socket.getRemoteSocketAddress().toString()
                : "unknown";

        try (Socket clientSocket = socket;
             InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

            while (true) {
                List<String> request = respParser.parse(reader);
                if (request == null || request.isEmpty()) {
                    break;
                }

                String response = commandRouter.route(request);
                writer.write(response);
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("I/O error on connection " + remote + ": " + e.getMessage());
        } finally {
            System.out.println("Client disconnected: " + remote);
        }
    }
}
