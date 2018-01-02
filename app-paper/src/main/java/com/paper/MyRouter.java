// Copyright (c) 2017-present CardinalBlue
//
// Author: jack.huang@cardinalblue.com
//         boy@cardinalblue.com
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

package com.paper;

import android.os.Looper;
import java.util.HashMap;
import ru.terrakok.cicerone.BaseRouter;
import ru.terrakok.cicerone.Navigator;
import ru.terrakok.cicerone.commands.Back;
import ru.terrakok.cicerone.commands.BackTo;
import ru.terrakok.cicerone.commands.Forward;
import ru.terrakok.cicerone.commands.Replace;
import ru.terrakok.cicerone.commands.SystemMessage;
import ru.terrakok.cicerone.result.ResultListener;

public class MyRouter extends BaseRouter {

    private HashMap<Integer, ResultListener> mResultListeners = new HashMap<>();
    private HashMap<Integer, Object> mResultsBuffer = new HashMap<>();

    public MyRouter() {
        super();
    }

    /**
     * Subscribe to the screen result.<br>
     * <b>Note:</b> only one listener can subscribe to a unique requestCode!<br>
     * You must call a <b>removeResultListener()</b> to avoid a memory leak.
     *
     * @param requestCode key for filter results
     * @param listener   result listener
     */
    public void setResultListener(Integer requestCode, ResultListener listener) {
        mResultListeners.put(requestCode, listener);
    }

    /**
     * Call this in onResume to get the buffer result by requestCode.
     *
     * @param requestCode The request code for the result.
     */
    public void dispatchResultOnResume(Integer requestCode) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        Object result = mResultsBuffer.get(requestCode);
        ResultListener resultListener = mResultListeners.get(requestCode);

        if (result != null && resultListener != null) {
            resultListener.onResult(result);
            // Remove the buffer.
            mResultsBuffer.remove(requestCode);
        }
    }

    /**
     * Unsubscribe from the screen result.
     *
     * @param requestCode key for filter results
     */
    public void removeResultListener(Integer requestCode) {
        mResultListeners.remove(requestCode);
    }

    /**
     * Send a delayed result until the {@link #dispatchResultOnResume(Integer)} is called.
     *
     * @param requestCode result data key
     * @param result     result data
     */
    protected void sendDelayedResult(Integer requestCode, Object result) {
        mResultsBuffer.put(requestCode, result);
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
     * Return to the previous screen in the chain and send result data.
     *
     * @param requestCode result data key
     * @param result     result data
     */
    public void exitWithResult(Integer requestCode, Object result) {
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
}
