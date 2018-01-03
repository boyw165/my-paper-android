// Copyright Jan 2018-present CardinalBlue
//
// Author: jack.huang@cardinalblue.com
//         boy@cardinalblue.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.router;

import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import ru.terrakok.cicerone.BaseRouter;
import ru.terrakok.cicerone.Navigator;
import ru.terrakok.cicerone.commands.Back;
import ru.terrakok.cicerone.commands.BackTo;
import ru.terrakok.cicerone.commands.Forward;
import ru.terrakok.cicerone.commands.Replace;
import ru.terrakok.cicerone.commands.SystemMessage;
import ru.terrakok.cicerone.result.ResultListener;

public class MyRouter extends BaseRouter {

    private Map<Integer, ListenerAndResult> mResultsBuffer = new HashMap<>();

    public MyRouter() {
        super();
    }

    /**
     * Subscribe to the screen resultHolder.<br>
     * <b>Note:</b> only one listenerHolder can subscribe to a unique requestCode!<br>
     * You must call a <b>removeResultListener()</b> to avoid a memory leak.
     *
     * @param requestCode key for filter results
     * @param listener   resultHolder listenerHolder
     */
    public void addResultListener(int requestCode, ResultListener listener) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        if (mResultsBuffer.containsKey(requestCode)) {
            final ListenerAndResult buffer = mResultsBuffer.get(requestCode);
            buffer.listenerHolder.set(listener);
        } else {
            final ListenerAndResult buffer = new ListenerAndResult();
            buffer.listenerHolder.set(listener);

            mResultsBuffer.put(requestCode, buffer);
        }
    }

    /**
     * Unsubscribe from the screen resultHolder.
     *
     * @param requestCode key for filter results
     */
    public void removeResultListener(int requestCode) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        if (mResultsBuffer.containsKey(requestCode)) {
            mResultsBuffer.remove(requestCode);
        }
    }

    /**
     * Call this in onResume to get the buffer resultHolder by requestCode.
     */
    public void dispatchResultOnResume() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        for (Integer requestCode : mResultsBuffer.keySet()) {
            final ListenerAndResult buffer = mResultsBuffer.get(requestCode);
            final Object result = buffer.resultHolder.get();
            final ResultListener listener = buffer.listenerHolder.get();
            if (result != null && listener != null) {
                listener.onResult(result);
                // Clear the cache.
                buffer.resultHolder.set(null);
            }
        }
    }

    /**
     * Open new screen and add it to the screens chain.
     *
     * @param screenKey screen key
     */
    public void navigateTo(String screenKey) {
        navigateTo(screenKey, null);
    }

    /**
     * Open new screen and add it to screens chain.
     *
     * @param screenKey screen key
     * @param data      initialisation parameters for the new screen
     */
    public void navigateTo(String screenKey, Object data) {
        executeCommand(new Forward(screenKey, data));
    }

    /**
     * Clear the current screens chain and start new one
     * by opening a new screen right after the root.
     *
     * @param screenKey screen key
     */
    public void newScreenChain(String screenKey) {
        newScreenChain(screenKey, null);
    }

    /**
     * Clear the current screens chain and start new one
     * by opening a new screen right after the root.
     *
     * @param screenKey screen key
     * @param data      initialisation parameters for the new screen
     */
    public void newScreenChain(String screenKey, Object data) {
        executeCommand(new BackTo(null));
        executeCommand(new Forward(screenKey, data));
    }

    /**
     * Clear all screens and open new one as root.
     *
     * @param screenKey screen key
     */
    public void newRootScreen(String screenKey) {
        newRootScreen(screenKey, null);
    }

    /**
     * Clear all screens and open new one as root.
     *
     * @param screenKey screen key
     * @param data      initialisation parameters for the root
     */
    public void newRootScreen(String screenKey, Object data) {
        executeCommand(new BackTo(null));
        executeCommand(new Replace(screenKey, data));
    }

    /**
     * Replace current screen.
     * By replacing the screen, you alters the backstack,
     * so by going back you will return to the previous screen
     * and not to the replaced one.
     *
     * @param screenKey screen key
     */
    public void replaceScreen(String screenKey) {
        replaceScreen(screenKey, null);
    }

    /**
     * Replace current screen.
     * By replacing the screen, you alters the backstack,
     * so by going back you will return to the previous screen
     * and not to the replaced one.
     *
     * @param screenKey screen key
     * @param data      initialisation parameters for the new screen
     */
    public void replaceScreen(String screenKey, Object data) {
        executeCommand(new Replace(screenKey, data));
    }

    /**
     * Return back to the needed screen from the chain.
     * Behavior in the case when no needed screens found depends on
     * the processing of the {@link BackTo} command in a {@link Navigator} implementation.
     *
     * @param screenKey screen key
     */
    public void backTo(String screenKey) {
        executeCommand(new BackTo(screenKey));
    }

    /**
     * Remove all screens from the chain and exit.
     * It's mostly used to finish the application or close a supplementary navigation chain.
     */
    public void finishChain() {
        executeCommand(new BackTo(null));
        executeCommand(new Back());
    }

    /**
     * Return to the previous screen in the chain.
     * Behavior in the case when the current screen is the root depends on
     * the processing of the {@link Back} command in a {@link Navigator} implementation.
     */
    public void exit() {
        executeCommand(new Back());
    }

    /**
     * Return to the previous screen in the chain and send resultHolder data.
     *
     * @param requestCode resultHolder data key
     * @param result     resultHolder data
     */
    public void exitWithResult(int requestCode, Object result) {
        exit();
        sendDelayedResult(requestCode, result);
    }

    /**
     * Return to the previous screen in the chain and show system message.
     *
     * @param message message to show
     */
    public void exitWithMessage(String message) {
        executeCommand(new Back());
        executeCommand(new SystemMessage(message));
    }

    /**
     * Show system message.
     *
     * @param message message to show
     */
    public void showSystemMessage(String message) {
        executeCommand(new SystemMessage(message));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    /**
     * Send a delayed resultHolder until the {@link #dispatchResultOnResume()} is
     * called.
     *
     * @param requestCode resultHolder data key
     * @param result     resultHolder data
     */
    private void  sendDelayedResult(int requestCode, Object result) {
        if (mResultsBuffer.containsKey(requestCode)) {
            mResultsBuffer.get(requestCode)
                          .resultHolder
                          .set(result);
        } else {
            mResultsBuffer.put(requestCode, new ListenerAndResult(result));
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private static class ListenerAndResult {

        final AtomicReference<ResultListener> listenerHolder = new AtomicReference<>();
        final AtomicReference<Object> resultHolder = new AtomicReference<>();

        ListenerAndResult() {
            // EMPTY constructor.
        }

        ListenerAndResult(Object result) {
            resultHolder.set(result);
        }
    }
}
