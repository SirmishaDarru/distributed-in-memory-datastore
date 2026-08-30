package com.datastore.protocol;

/**
 * Serializes Java values into RESP reply strings to send back to a client.
 */
public final class RespSerializer {

    private RespSerializer() {
    }

    public static String serializeSimpleString(String data) {
        return "+" + data + "\r\n";
    }

    public static String serializeBulkString(String data) {
        if (data == null) {
            return "$-1\r\n";
        }
        return "$" + data.length() + "\r\n" + data + "\r\n";
    }

    public static String serializeError(String message) {
        return "-" + message + "\r\n";
    }
}
