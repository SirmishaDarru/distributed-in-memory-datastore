package com.datastore.command;

import java.util.List;

/**
 * A datastore command that consumes parsed RESP arguments and returns a
 * serialized RESP reply.
 */
public interface Command {

    String execute(List<String> args);
}
