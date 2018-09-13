package com.paper.model.observables

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class TestObservableCollection<T>(private val actual: MutableCollection<T>,
                                  override val size: Int)
    : MutableCollection<T> {

    // TODO: Make it thread-safe

    override fun contains(element: T): Boolean {
        return actual.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return actual.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return actual.isEmpty()
    }

    private val addSignal = PublishSubject.create<T>().toSerialized()
    private val removeSignal = PublishSubject.create<T>().toSerialized()

    val itemAdded: Observable<T> get() = addSignal.hide()
    val itemRemoved: Observable<T> get() = removeSignal.hide()

    override fun add(element: T): Boolean {
        val successful = actual.add(element)

        if (successful) {
            addSignal.onNext(element)
        }

        return successful
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val successful = actual.addAll(elements)

        if (successful) {
            elements.forEach { addSignal.onNext(it) }
        }

        return successful
    }

    override fun remove(element: T): Boolean {
        val successful = actual.remove(element)

        if (successful) {
            removeSignal.onNext(element)
        }

        return successful
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val successful = actual.removeAll(elements)

        if (successful) {
            elements.forEach { removeSignal.onNext(it) }
        }

        return successful
    }

    override fun clear() {
        val removed = actual.toList()

        actual.clear()

        removed.forEach { removeSignal.onNext(it) }
    }

    override fun iterator(): MutableIterator<T> {
        TODO("not implemented")
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        TODO("not implemented")
    }
}
