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

package com.cardinalblue.lib.doodle.protocol;

public interface ILogger {

    /**
     * Send a DEBUG log message.
     *
     * @param tag Used to identify the source of a log message.  It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    int d(String tag, String msg);

    /**
     * Send an ERROR log message.
     *
     * @param tag Used to identify the source of a log message.  It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    int e(String tag, String msg);

    /**
     * Send event to the remote analytics server.
     *
     * @param action     The key.
     * @param parameters The parameters with the key.
     */
    void sendEvent(String action, String... parameters);
}
