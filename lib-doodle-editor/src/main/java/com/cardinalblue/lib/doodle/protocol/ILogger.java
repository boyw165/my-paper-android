package com.cardinalblue.lib.doodle.protocol;

public interface ILogger {

    void d(String tag, String message);

    void sendEvent(String... messages);
}
