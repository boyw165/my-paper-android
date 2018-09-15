// Copyright Feb 2018-present boyw165@gmail.com
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

package com.paper.view.canvas

import com.paper.model.repository.IBitmapRepository
import io.reactivex.Maybe
import java.io.File

/**
 * A behavior that enables the view to write the thumbnail to a file storing in
 * the repository. See [IBitmapRepository]
 */
interface IWriteThumbnailFileCanvasView {

    /**
     * Inject the Bitmap repository so that this component is able to
     */
    fun injectBitmapRepository(repo: IBitmapRepository)

    /**
     * Write the thumbnail Bitmap to a file maintained by the Bitmap repository,
     * see [injectBitmapRepository] method.
     *
     * @return A triple represents the [File], thumbnail width and height.
     */
    fun writeThumbFileToBitmapRepository(): Maybe<Triple<File, Int, Int>>
}
