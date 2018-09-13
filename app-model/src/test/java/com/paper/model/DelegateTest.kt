package com.paper.model

import com.paper.model.observables.TestDelegate
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.Silent::class)
class DelegateTest {

    val foo by TestDelegate<List<Int>>()

    @Test
    fun test() {
        foo
    }
}
