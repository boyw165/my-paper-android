// Copyright (c) 2017-present boyw165
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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.cardinalblue.lib.doodle.SketchEditorActivity;
import com.cardinalblue.lib.doodle.data.SketchModel;

public class PaperEditorActivity extends AppCompatActivity {

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    @Override
    protected void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState);

        // FIXME: Workaround of creating a new paper model and navigate to the
        // FIXME: sketch editor immediately.
        startActivityForResult(
            new Intent(this, SketchEditorActivity.class)
                // Pass a sketch struct.
                .putExtra(SketchEditorActivity.PARAMS_SKETCH_STRUCT, new SketchModel(0, 500, 500))
                // Pass a sketch background.
//                .putExtra(SketchEditorActivity.PARAMS_BACKGROUND_FILE, background)
                // Remembering brush color and stroke width.
//                .putExtra(SketchEditorActivity.PARAMS_REMEMBERING_BRUSH_COLOR, brushColor)
//                .putExtra(SketchEditorActivity.PARAMS_REMEMBERING_BRUSH_SIZE, brushSize)
                // Alert message.
                .putExtra(SketchEditorActivity.PARAMS_ALERT_TITLE_MESSAGE, getString(R.string.doodle_clear_title))
                .putExtra(SketchEditorActivity.PARAMS_ALERT_CONFIRM_MESSAGE, getString(R.string.doodle_clear_message))
                .putExtra(SketchEditorActivity.PARAMS_ALERT_POSITIVE_MESSAGE, getString(R.string.doodle_clear_ok))
                .putExtra(SketchEditorActivity.PARAMS_ALERT_NEGATIVE_MESSAGE, getString(R.string.doodle_clear_cancel))
                // Ask the editor enter fullscreen mode.
                .putExtra(SketchEditorActivity.PARAMS_FULLSCREEN_MODE, false)
                // DEBUG mode.
                .putExtra(SketchEditorActivity.PARAMS_DEBUG_MODE, true),
            0);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // FIXME: Workaround of leaving the editor immediately.
        finish();
    }
}
