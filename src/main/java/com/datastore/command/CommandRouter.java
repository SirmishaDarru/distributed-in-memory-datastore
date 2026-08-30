package com.datastore.command;

import com.datastore.protocol.RespSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry that maps command names to {@link Command} implementations.
 */
public class CommandRouter {

    private final Map<String, Command> commands;

    public CommandRouter() {
        this.commands = new HashMap<>();
        this.commands.put("PING", new PingCommand());
        this.commands.put("ECHO", new EchoCommand());
    }

    /**
     * Dispatches a parsed RESP request. The first element is the command name;
     * remaining elements are arguments.
     */
    public String route(List<String> parsedRequest) {
        if (parsedRequest == null || parsedRequest.isEmpty() || parsedRequest.get(0) == null) {
            return RespSerializer.serializeError("ERR empty command");
        }

        String commandName = parsedRequest.get(0).toUpperCase(Locale.ROOT);
        Command command = commands.get(commandName);
        if (command == null) {
            return RespSerializer.serializeError("ERR unknown command '" + parsedRequest.get(0) + "'");
        }

        List<String> args = parsedRequest.subList(1, parsedRequest.size());
        return command.execute(args);
    }
}
