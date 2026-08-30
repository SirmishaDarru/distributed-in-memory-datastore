package com.datastore.command;

import com.datastore.protocol.RespSerializer;

import java.util.List;

/**
 * Redis-compatible {@code ECHO}. Requires exactly one argument and returns it
 * as a bulk string.
 */
public class EchoCommand implements Command {

    @Override
    public String execute(List<String> args) {
        if (args == null || args.isEmpty()) {
            return RespSerializer.serializeError("ERR wrong number of arguments for 'echo' command");
        }
        return RespSerializer.serializeBulkString(args.get(0));
    }
}
