package com.datastore.command;

import com.datastore.protocol.RespSerializer;

import java.util.List;

/**
 * Redis-compatible {@code PING}. With no arguments replies {@code +PONG}.
 * With an argument, echoes that argument as a bulk string.
 */
public class PingCommand implements Command {

    @Override
    public String execute(List<String> args) {
        if (args == null || args.isEmpty()) {
            return RespSerializer.serializeSimpleString("PONG");
        }
        return RespSerializer.serializeBulkString(args.get(0));
    }
}
