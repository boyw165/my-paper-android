package com.paper;

import android.graphics.PointF;

import com.paper.domain.util.TransformUtils;

import org.junit.Assert;
import org.junit.Test;

public class TransformUtilsTest {

    @Test
    public void orientation_isCorrect() throws Exception {
        // ^
        // |
        // | 3
        // |      2
        // |1
        // +--------->
        Assert.assertEquals(TransformUtils.Companion.getORIENTATION_CCW(),
                            TransformUtils.Companion.getOrientation(
                                new PointF(1f, 1f),
                                new PointF(6f, 2f),
                                new PointF(2f, 3f)
                            ));
        // ^
        // |
        // | 2
        // |      3
        // |1
        // +--------->
        Assert.assertEquals(TransformUtils.Companion.getORIENTATION_CW(),
                            TransformUtils.Companion.getOrientation(
                                new PointF(1f, 1f),
                                new PointF(2f, 3f),
                                new PointF(6f, 2f)
                            ));
        // ^
        // |
        // |      3
        // |   2
        // |1
        // +--------->
        Assert.assertEquals(TransformUtils.Companion.getORIENTATION_COLLINEAR(),
                            TransformUtils.Companion.getOrientation(
                                new PointF(1f, 1f),
                                new PointF(2f, 2f),
                                new PointF(3f, 3f)
                            ));
    }
}
