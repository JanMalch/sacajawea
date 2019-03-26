package io.github.janmalch.sacajawea.observable

import java.util.*

interface IObservable<T> {
    /** register handler for next values */
    fun subscribe(observer: (T) -> Unit)
    /** register handler for completion of observable */
    fun completed(handler: () -> Unit)
}

open class Subject<T> : IObservable<T> {
    private val observers = ArrayList<(T) -> Unit>()
    private val completions = ArrayList<() -> Unit>()

    override fun subscribe(observer: (T) -> Unit) {
        observers.add(observer)
    }

    override fun completed(handler: () -> Unit) {
        completions.add(handler)
    }

    open fun complete() {
        for (completion in completions) {
            completion()
        }
        observers.clear()
    }

    fun toObservable(): IObservable<T> {
        return Observable(this)
    }

    fun next(t: T) {
        for (observer in observers) {
            observer(t)
        }
    }
}

class Observable<T>(private val source: Subject<T>) :
    IObservable<T> by source