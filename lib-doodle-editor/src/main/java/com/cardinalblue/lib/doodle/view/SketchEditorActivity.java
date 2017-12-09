// Copyright (c) 2017-present CardinalBlue
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

package com.cardinalblue.lib.doodle.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.cardinalblue.lib.doodle.observables.AlertDialogObservable;
import com.cardinalblue.lib.doodle.DummyLogger;
import com.cardinalblue.lib.doodle.R;
import com.cardinalblue.lib.doodle.observables.SeekBarChangeObservable;
import com.cardinalblue.lib.doodle.DrawStrokeManipulator;
import com.cardinalblue.lib.doodle.PinchCanvasManipulator;
import com.cardinalblue.lib.doodle.SketchEditorPresenter;
import com.cardinalblue.lib.doodle.SketchUndoRedoManipulator;
import com.cardinalblue.lib.doodle.event.UiEvent;
import com.cardinalblue.lib.doodle.event.UiTouchEvent;
import com.cardinalblue.lib.doodle.gesture.GestureRecognizer;
import com.cardinalblue.lib.doodle.gesture.MotionEvent2TouchEventMapper;
import com.cardinalblue.lib.doodle.ILogger;
import com.cardinalblue.lib.doodle.ISketchBrush;
import com.cardinalblue.lib.doodle.SketchContract;
import com.cardinalblue.lib.doodle.view.adapter.BrushAdapter;
import com.cardinalblue.lib.doodle.view.adapter.BrushAdapterObservable;
import com.jakewharton.rxbinding2.view.RxView;
import com.paper.shared.model.repository.SketchRepo;
import com.paper.shared.model.sketch.Sketch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class SketchEditorActivity
    extends RxAppCompatActivity
    implements SketchContract.IEditorView {

    /**
     * Sketch width. The width property is not stored in the database because
     * the scrap has it already.
     */
    public static final String PARAMS_SKETCH_WIDTH = "sketch_width";
    /**
     * Sketch height. The height property is not stored in the database because
     * the scrap has it already.
     */
    public static final String PARAMS_SKETCH_HEIGHT = "sketch_height";
    /**
     * Background in the sketch.
     */
    public static final String PARAMS_BACKGROUND_FILE = "background_file";
    /**
     * Background in the sketch.
     */
    public static final String PARAMS_BACKGROUND_URI = "background_uri";
    /**
     * The previous brush color.
     */
    public static final String PARAMS_REMEMBERING_BRUSH_COLOR = "previous_brush_color";
    /**
     * The previous brush size (stroke width).
     */
    public static final String PARAMS_REMEMBERING_BRUSH_SIZE = "previous_brush_size";
    /**
     * Whether to run the editor in fullscreen mode.
     */
    public static final String PARAMS_FULLSCREEN_MODE = "fullscreen_mode";
    /**
     * Whether to show DEBUG information on the canvas.
     */
    public static final String PARAMS_DEBUG_MODE = "debug_mode";

    private static final String SAVED_SKETCH_MODEL = "saved_sketch_model";

    // View.
    View mNavigationBar;
    View mBtnClose;
    View mBtnClear;
    View mBtnUndo;
    View mBtnRedo;
    View mBtnDone;
    BrushSizeSeekBar mBrushSizePicker;
    RecyclerView mBrushPicker;
    View mBottomBarBackground;
    SketchView mSketchView;
    ProgressDialog mProgressDialog;

    // Adapter.
    BrushAdapter mBrushPickerAdapter;
    // Image Loader.
    RequestManager mGlide;

    // Logger.
    ILogger mLogger;

    // Gesture recognizer.
    GestureRecognizer mGestureRecognizer;

    // Controllers.
    SketchContract.ISketchEditorPresenter mEditorPresenter;

    // Fullscreen mode.
    boolean mFullscreenMode = false;

    // Animation state.
    boolean mIsAnimating = false;
    AnimatorSet mAnimSet = null;

    // Alert dialog message.
    private String mAlertTitle;
    private String mAlertMessage;
    private String mAlertPositiveMessage;
    private String mAlertNegativeMessage;

    @Override
    public void showProgress(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
        }

        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    @Override
    public void hideProgress() {
        if (mProgressDialog == null) return;

        mProgressDialog.dismiss();
    }

    @Override
    public void showErrorAlertThenClose(Throwable error) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Error")
               .setMessage(error.getMessage())
               .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       finish();
                   }
               })
               .show();
    }

    @Override
    public void close() {
        if (isFinishing()) return;

        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void closeWithUpdate(Sketch model,
                                int brushColor,
                                int strokeWidth) {
        if (isFinishing()) return;

        // TODO: Write to the repository.
//        setResult(RESULT_OK,
//                  new Intent().putExtra(PARAMS_SKETCH_STRUCT, model)
//                              .putExtra(PARAMS_REMEMBERING_BRUSH_COLOR, brushColor)
//                              .putExtra(PARAMS_REMEMBERING_BRUSH_SIZE, strokeWidth));
        setResult(RESULT_OK,
                  new Intent().putExtra(PARAMS_REMEMBERING_BRUSH_COLOR, brushColor)
                              .putExtra(PARAMS_REMEMBERING_BRUSH_SIZE, strokeWidth));
        finish();
    }

    @Override
    public void setBrushSize(int brushSize) {
        mBrushSizePicker.setProgress(brushSize);
    }

    @Override
    public void setBrushItems(List<ISketchBrush> brushes) {
        mBrushPickerAdapter.setItems(brushes);
    }

    @Override
    public void selectBrushAt(int defaultSelection) {
        mBrushPickerAdapter.selectItem(defaultSelection);

        // In order to show ascendants of selected one, we shift the showing
        // position toward left a little bit.
        final int position = Math.max(0, defaultSelection - 3);
        mBrushPicker.scrollToPosition(position);
    }

    @Override
    public void showBrushColor(int color) {
        mBrushSizePicker.showStrokeColor(color);
    }

    @Override
    public void showStrokeColorAndWidthPreview(int color) {
        mBrushSizePicker.showStrokeColorAndWidthPreview(color);
    }

    @Override
    public void hideStrokeColorAndWidthPreview() {
        mBrushSizePicker.hidePreviewColor();
    }

    @Override
    public void showOrHideDoneButton(boolean showed) {
        if (showed) {
            mBtnDone.setVisibility(View.VISIBLE);
            mBtnDone.setClickable(true);
        } else {
            mBtnDone.setVisibility(View.INVISIBLE);
            mBtnDone.setClickable(false);
        }
    }

    @Override
    public void showOrHideUndoButton(boolean showed, boolean enabled) {
        if (showed) {
            mBtnUndo.setVisibility(View.VISIBLE);
            mBtnUndo.setClickable(true);
        } else {
            mBtnUndo.setVisibility(View.INVISIBLE);
            mBtnUndo.setClickable(false);
        }

        if (enabled) {
            mBtnUndo.setAlpha(1f);
        } else {
            mBtnUndo.setAlpha(0.5f);
        }
    }

    @Override
    public void showOrHideRedoButton(boolean showed, boolean enabled) {
        if (showed) {
            mBtnRedo.setVisibility(View.VISIBLE);
            mBtnRedo.setClickable(true);
        } else {
            mBtnRedo.setVisibility(View.INVISIBLE);
            mBtnRedo.setClickable(false);
        }

        if (enabled) {
            mBtnRedo.setAlpha(1f);
        } else {
            mBtnRedo.setAlpha(0.5f);
        }
    }

    @Override
    public void showOrHideClearButton(boolean showed) {
        if (showed) {
            mBtnClose.setVisibility(View.GONE);
            mBtnClear.setVisibility(View.VISIBLE);
        } else {
            mBtnClose.setVisibility(View.VISIBLE);
            mBtnClear.setVisibility(View.GONE);
        }
    }

    @Override
    public void enableFullscreenMode(boolean enabled) {
        if (enabled) {
            if (!mFullscreenMode) {
                if (mAnimSet != null) {
                    mAnimSet.cancel();
                }
                mAnimSet = new AnimatorSet();
                mAnimSet.setDuration(150)
                        .playTogether(
                            ObjectAnimator.ofFloat(mNavigationBar, "alpha", 0f),
                            ObjectAnimator.ofFloat(mBrushSizePicker, "alpha", 0f),
                            ObjectAnimator.ofFloat(mBrushPicker, "alpha", 0f),
                            ObjectAnimator.ofFloat(mBottomBarBackground, "alpha", 0f));
                mAnimSet.addListener(mAnimationListener);
                mAnimSet.start();
            }
        } else {
            if (mAnimSet != null) {
                mAnimSet.cancel();
            }
            mAnimSet = new AnimatorSet();
            mAnimSet.setDuration(150)
                    .playTogether(
                        ObjectAnimator.ofFloat(mNavigationBar, "alpha", 1f),
                        ObjectAnimator.ofFloat(mBrushSizePicker, "alpha", 1f),
                        ObjectAnimator.ofFloat(mBrushPicker, "alpha", 1f),
                        ObjectAnimator.ofFloat(mBottomBarBackground, "alpha", 1f));
            mAnimSet.addListener(mAnimationListener);
            mAnimSet.start();
        }

        mFullscreenMode = enabled;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        // Set content view.
        setContentView(R.layout.activity_sketch_editor);

        // Common...
        final Intent intent = getIntent();
        if (getExternalCacheDir() == null) {
            throw new IllegalArgumentException(
                "Cannot access external cache directory.");
        }

        // Fullscreen mode.
        if (intent.getBooleanExtra(PARAMS_FULLSCREEN_MODE, false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                 WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Sketch width and height.
        final int sketchWidth = intent.getIntExtra(PARAMS_SKETCH_WIDTH, 0);
        final int sketchHeight = intent.getIntExtra(PARAMS_SKETCH_HEIGHT, 0);
        if (sketchWidth <= 0 || sketchHeight <= 0) {
            throw new IllegalArgumentException(
                "Sketch width or height is zero or negative.");
        }

        // Views.
        mNavigationBar = findViewById(R.id.navigation_bar);
        mBtnClose = findViewById(R.id.btn_close);
        mBtnClear = findViewById(R.id.btn_clear);
        mBtnUndo = findViewById(R.id.btn_undo);
        mBtnRedo = findViewById(R.id.btn_redo);
        mBtnDone = findViewById(R.id.btn_done);
        mBrushSizePicker = findViewById(R.id.brush_size_picker);
        mBrushPicker = findViewById(R.id.brush_picker);
        mBottomBarBackground = findViewById(R.id.bottom_bar_background);
        mSketchView = findViewById(R.id.sketch_editor);

        // Glide request manager.
        mGlide = Glide.with(this);

        // Sketch view.
        mSketchView.setCanvasConstraintPadding(0, mNavigationBar.getLayoutParams().height,
                                               0, mBottomBarBackground.getLayoutParams().height);
        mSketchView.setDebug(intent.getBooleanExtra(PARAMS_DEBUG_MODE, false));

        // Brush picker.
        mBrushPickerAdapter = new BrushAdapter(
            this, mGlide,
            getResources().getDimension(R.dimen.brush_border_width),
            getResources().getDimension(R.dimen.brush_selected_border_width));
        mBrushPicker.setLayoutManager(new LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false));
        mBrushPicker.setHasFixedSize(true);
        mBrushPicker.setAdapter(mBrushPickerAdapter);
        // Setup over-scroll decoration.
        OverScrollDecoratorHelper.setUpOverScroll(
            mBrushPicker, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);

        // Resource for showing alert dialog of clearing sketch.
        mAlertTitle = getResources().getString(R.string.doodle_clear_title);
        mAlertMessage = getResources().getString(R.string.doodle_clear_message);
        mAlertPositiveMessage = getResources().getString(R.string.doodle_clear_ok);
        mAlertNegativeMessage = getResources().getString(R.string.doodle_clear_cancel);

        // Init logger.
        // FIXME: Use Dagger2.
        mLogger = new DummyLogger();

        // Init gesture recognizer.
        final float dragSlop = getResources().getDimension(R.dimen.drag_slop);
        mGestureRecognizer = new GestureRecognizer(mSketchView, dragSlop, mLogger);

        // Init editor controller.
        final float minPathSegmentLength = getResources().getDimension(
            R.dimen.sketch_min_path_segment_length);
        final long minPathSegmentInterval = getResources().getInteger(
            R.integer.sketch_min_path_segment_interval);
        final float minStrokeWidth = getResources().getDimension(R.dimen.sketch_min_stroke_width);
        final float maxStrokeWidth = getResources().getDimension(R.dimen.sketch_max_stroke_width);

        mEditorPresenter = new SketchEditorPresenter(
            new SketchRepo(getPackageName(),
                           getContentResolver(),
                           getExternalCacheDir(),
                           Schedulers.io()),
            this,
            mSketchView,
            new DrawStrokeManipulator(minPathSegmentLength,
                                      minPathSegmentInterval,
                                      minStrokeWidth,
                                      maxStrokeWidth,
                                      Schedulers.computation(),
                                      AndroidSchedulers.mainThread(),
                                      mLogger),
            new PinchCanvasManipulator(Schedulers.computation(),
                                       AndroidSchedulers.mainThread(),
                                       mLogger),
            new SketchUndoRedoManipulator(Schedulers.computation(),
                                          AndroidSchedulers.mainThread(),
                                          mLogger),
            Schedulers.computation(),
            AndroidSchedulers.mainThread(),
            mLogger);

        // FIXME: Race condition (single source of truth)
        // Prepare the initial strokes.
        mDisposables.add(
            mEditorPresenter
                .initEditorAndLoadSketch(
                    sketchWidth,
                    sketchHeight,
                    intent.getIntExtra(PARAMS_REMEMBERING_BRUSH_COLOR, 0),
                    intent.getIntExtra(PARAMS_REMEMBERING_BRUSH_SIZE, -1))
                .subscribe());

        // FIXME: Race condition (single source of truth)
        // Create canvas resource...
        mDisposables.add(
            loadBackground(getIntent())
                .compose(mEditorPresenter.setBackground())
                .subscribe());

        // Canvas touch.
        mDisposables.add(
            onTouchCanvas()
                .compose(mGestureRecognizer)
                .compose(mEditorPresenter.touchCanvas())
                .subscribe());

        // Brush picker.
        mDisposables.add(
            onUpdateBrush()
                .compose(mEditorPresenter.setBrush())
                .subscribe());

        // Stroke width picker.
        mDisposables.add(
            onUpdateBrushSize()
                .compose(mEditorPresenter.setStrokeWidth())
                .subscribe());

        // Close button.
        mDisposables.add(
            onClickBack()
                .compose(mEditorPresenter.close())
                .subscribe());

        // Next button.
        mDisposables.add(
            onClickDone()
                .compose(mEditorPresenter.done())
                .subscribe());

        // Clear button.
        mDisposables.add(
            onClickClear()
                .compose(mEditorPresenter.clearStrokes())
                .subscribe());

        // Undo/Redo button.
        mDisposables.add(
            onClickUndo()
                .compose(mEditorPresenter.undo())
                .subscribe());
        mDisposables.add(
            onClickRedo()
                .compose(mEditorPresenter.redo())
                .subscribe());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Finish the remaining Glide request.
        mGlide.onDestroy();

        // Dispose all disposables.
        mDisposables.clear();

        // Force to hide progress.
        hideProgress();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mLogger.d("bread crumbs", "Serialize sketch model in Activity#save().");

        // FIXME: Use repo to save the sketch.
        mEditorPresenter.saveSketch();
    }

    private Observable<Object> onClickBack() {
        return Observable
            .mergeArray(RxView.clicks(mBtnClose),
                        onClickSystemBack())
            // Don't emit event while animating.
            .filter(new Predicate<Object>() {
                @Override
                public boolean test(Object o)
                    throws Exception {
                    return !mIsAnimating;
                }
            })
            .debounce(150, TimeUnit.MILLISECONDS);
    }

    private Observable<Object> onClickClear() {
        return RxView.clicks(mBtnClear)
                     // Don't emit event while animating.
                     .filter(new Predicate<Object>() {
                         @Override
                         public boolean test(Object o)
                             throws Exception {
                             return !mIsAnimating;
                         }
                     })
                     .debounce(150, TimeUnit.MILLISECONDS)
                     .flatMap(new Function<Object, ObservableSource<?>>() {
                         @Override
                         public ObservableSource<?> apply(Object ignored)
                             throws Exception {
                             // Send analytics event.
                             mLogger.sendEvent("Doodle editor - tap clear");

                             // Build a alert-dialog.
                             final AlertDialog.Builder builder =
                                 new AlertDialog.Builder(SketchEditorActivity.this)
                                     .setTitle(mAlertTitle)
                                     .setMessage(mAlertMessage);

                             // Convert the alert-dialog to an Observable.
                             return new AlertDialogObservable(
                                 builder, mAlertPositiveMessage, mAlertNegativeMessage)
                                 .subscribeOn(AndroidSchedulers.mainThread());
                         }
                     });
    }

    private Observable<Object> onClickDone() {
        return RxView.clicks(mBtnDone)
                     // Don't emit event while animating.
                     .filter(new Predicate<Object>() {
                         @Override
                         public boolean test(Object o)
                             throws Exception {
                             return !mIsAnimating;
                         }
                     })
                     .debounce(150, TimeUnit.MILLISECONDS);
    }

    private Observable<Object> onClickUndo() {
        return RxView.clicks(mBtnUndo)
                     // Don't emit event while animating.
                     .filter(new Predicate<Object>() {
                         @Override
                         public boolean test(Object o)
                             throws Exception {
                             return !mIsAnimating;
                         }
                     })
                     .debounce(150, TimeUnit.MILLISECONDS);
    }

    private Observable<Object> onClickRedo() {
        return RxView.clicks(mBtnRedo)
                     // Don't emit event while animating.
                     .filter(new Predicate<Object>() {
                         @Override
                         public boolean test(Object o)
                             throws Exception {
                             return !mIsAnimating;
                         }
                     })
                     .debounce(150, TimeUnit.MILLISECONDS);
    }

    private Observable<UiTouchEvent> onTouchCanvas() {
        return RxView.touches(mSketchView)
                     .compose(new MotionEvent2TouchEventMapper());
    }

    private Observable<UiEvent<Integer>> onUpdateBrushSize() {
        return new SeekBarChangeObservable(mBrushSizePicker)
            // Don't emit event while animating.
            .filter(new Predicate<UiEvent<Integer>>() {
                @Override
                public boolean test(UiEvent<Integer> ignored)
                    throws Exception {
                    return !mIsAnimating;
                }
            });
    }

    private Observable<Integer> onUpdateBrush() {
        return new BrushAdapterObservable(mBrushPickerAdapter)
            // Don't emit event while animating.
            .filter(new Predicate<Integer>() {
                @Override
                public boolean test(Integer ignored)
                    throws Exception {
                    return !mIsAnimating;
                }
            })
            .debounce(150, TimeUnit.MILLISECONDS);
    }

    Observable<InputStream> loadBackground(final Intent intent) {
        // From file.
        final File backgroundFile = (File) intent.getSerializableExtra(PARAMS_BACKGROUND_FILE);
        // From URI.
        final Uri backgroundUri = intent.getParcelableExtra(PARAMS_BACKGROUND_URI);

        return Observable
            .fromCallable(new Callable<InputStream>() {
                @Override
                public InputStream call() throws Exception {
                    if (backgroundFile != null &&
                        backgroundFile.exists()) {
                        // Create canvas resource from file.
                        return new FileInputStream(backgroundFile);
                    } else if (backgroundUri != null) {
                        // TODO: Read file from ContentProvider.
                        // Create canvas resource from asset URI.
                        return getAssets().open(backgroundUri.getPathSegments().get(1));
                    } else {
                        throw new FileNotFoundException("No background is provided.");
                    }
                }
            })
            .subscribeOn(Schedulers.io());
    }

    Animator.AnimatorListener mAnimationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            mIsAnimating = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mIsAnimating = false;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            // DO NOTHING.
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            // DO NOTHING.
        }
    };
}
