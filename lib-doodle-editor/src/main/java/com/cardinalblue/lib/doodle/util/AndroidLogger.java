// Copyright (c) 2017-present Cardinalblue
//
// Author: boy@cardinalblue.com
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.cardinalblue.lib.doodle.util;

import android.util.Log;

import com.cardinalblue.lib.doodle.protocol.ILogger;
import com.piccollage.util.protocol.ILogEvent;

import java.util.Arrays;

public class AndroidLogger implements ILogger {

    private final ILogEvent mRemoteLogger;

    public AndroidLogger(ILogEvent remoteLogger) {
        mRemoteLogger = remoteLogger;
    }

    @Override
    public int d(String tag, String msg) {
        return Log.d(tag, msg);
    }

    @Override
    public int e(String tag, String msg) {
        return Log.e(tag, msg);
    }

    @Override
    public void sendEvent(String action, String... parameters) {
        if (parameters.length == 0) {
            Log.d("event", action + "");
        } else {
            Log.d("event", action + ": " + Arrays.toString(parameters));
        }

        if (mRemoteLogger != null) {
            mRemoteLogger.sendEvent(action, parameters);
        }
    }
}
