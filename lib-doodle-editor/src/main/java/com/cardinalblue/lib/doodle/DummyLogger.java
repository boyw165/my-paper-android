package com.cardinalblue.lib.doodle;

import android.util.Log;

import com.cardinalblue.lib.doodle.protocol.ILogger;

class DummyLogger implements ILogger {

    @Override
    public void d(String tag, String message) {
        Log.d(tag, message);
    }

    @Override
    public void sendEvent(String... messages) {
        final StringBuilder builder = new StringBuilder();
        for (String message : messages) {
            builder.append(message);
        }
        Log.d("event", builder.toString());
    }
}
