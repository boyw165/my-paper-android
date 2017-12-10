package com.paper.editor

import com.paper.protocol.IPresenter
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

class PaperEditorPresenter(controller: PaperController,
                           uiScheduler: Scheduler,
                           workerScheduler: Scheduler)
    : IPresenter<PaperEditorContract.View> {

    // Given.
    private val mPaperController: PaperController = controller
    private val mUiScheduler: Scheduler = uiScheduler
    private val mWorkerScheduler: Scheduler = workerScheduler
    private var mView: PaperEditorContract.View? = null

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    override fun bindViewOnCreate(view: PaperEditorContract.View) {
        mView = view

        // Close button.
        mDisposablesOnCreate.add(
            mView!!.onClickCloseButton()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(mUiScheduler)
                .subscribe {
                    mView?.close()
                })

        // Draw toggle button.
        mDisposablesOnCreate.add(
            mView!!.onClickDrawButton()
                .debounce(150, TimeUnit.MILLISECONDS)
                .observeOn(mUiScheduler)
                .subscribe { checked ->
                    // TODO: Configure paper controller to drawing mode.
                })
    }

    override fun unBindViewOnDestroy() {
        mDisposablesOnCreate.clear()

        mView = null
    }

    override fun onResume() {
//        mDisposablesOnResume.add()
    }

    override fun onPause() {
        mDisposablesOnResume.clear()
    }
}
