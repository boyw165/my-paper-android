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

package com.my.widget.gesture.state;

import android.os.Message;
import android.view.MotionEvent;

import com.my.widget.gesture.IGestureStateOwner;

public class MultipleFingersPressingState extends BaseGestureState {

    public MultipleFingersPressingState(IGestureStateOwner owner) {
        super(owner);
    }

    @Override
    public void onEnter(MotionEvent event,
                        Object touchingObject,
                        Object touchingContext) {
        // DO NOTHING.
    }

    @Override
    public void onDoing(MotionEvent event,
                        Object touchingObject,
                        Object touchingContext) {
        // DO NOTHING.
    }

    @Override
    public void onExit(MotionEvent event,
                       Object touchingObject,
                       Object touchingContext) {
        // DO NOTHING.
    }

    @Override
    public boolean onHandleMessage(Message msg) {
        return false;
    }
}
