//  Copyright Oct 2017-present CardinalBlue
//
//  Author: boy@cardinalblue.com
//          jack.huang@cardinalblue.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.my.widget.gesture;

import android.os.Handler;
import android.view.MotionEvent;

public interface IGestureStateOwner {

//    // constants for Message.what used by GestureHandler below
//    int MSG_ACTION_BEGIN = 0x0;
//    int MSG_ACTION_END = 0xFFFFFFFF;
//    int MSG_LONG_PRESS = 0xA1;
//    int MSG_LONG_TAP = 0xA2;
//    int MSG_TAP = 0xA3;
//    int MSG_FLING = 0xB1;
//    int MSG_DRAG_BEGIN = 0xB2;
//    int MSG_DRAGGING = 0xB3;
//    int MSG_DRAG_END = 0xB4;
//    int MSG_PINCH_BEGIN = 0xC1;
//    int MSG_PINCHING = 0xC2;
//    int MSG_PINCH_END = 0xC3;

    // All recognized states.
    enum State {
        STATE_IDLE,

        STATE_SINGLE_FINGER_PRESSING,
        STATE_TAP,
        STATE_LONG_TAP,
        STATE_LONG_PRESS,
        STATE_DRAG,
        STATE_FLING,

        STATE_MULTIPLE_FINGERS_PRESSING,
        STATE_PINCH
    }

    Handler getHandler();

    IGestureListener getListener();

    void issueStateTransition(State newState);

    void issueStateTransitionAndRun(State newState,
                                    MotionEvent event,
                                    Object touchingObject,
                                    Object touchingContext);
}
