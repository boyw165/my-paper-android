// Copyright (c) 2016-present boyw165
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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.jakewharton.rxbinding2.view.RxView;
import com.paper.shared.model.PaperModel;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;

public class MyPaperGalleryActivity extends AppCompatActivity {

    // View.
    @BindView(R.id.btn_new)
    View mBtnNewPaper;

    Unbinder mViewUnbinder;

    CompositeDisposable mDisposables;

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_paper_gallery);

        mViewUnbinder = ButterKnife.bind(this);

        mDisposables = new CompositeDisposable();
        mDisposables.add(
            RxView.clicks(mBtnNewPaper)
                  .debounce(150, TimeUnit.MILLISECONDS)
                  // TODO: MVP's Presenter's transformer.
                  .compose(new ObservableTransformer<Object, Object>() {
                      @Override
                      public ObservableSource<Object> apply(@NonNull Observable<Object> upstream) {
                          return upstream
                              .observeOn(AndroidSchedulers.mainThread())
                              .map(new Function<Object, Object>() {
                                  @Override
                                  public Object apply(@NonNull Object o) throws Exception {
                                      navigateToPaperEditor(new PaperModel());
                                      return o;
                                  }
                              });
                      }
                  })
                  .subscribe());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mViewUnbinder.unbind();

        mDisposables.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    // TODO: MVP's View method.
    private void navigateToPaperEditor(PaperModel paperModel) {
        startActivityForResult(new Intent(this, PaperEditorActivity.class), 0);
    }
}
