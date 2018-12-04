package io.github.janmalch.sacajawea.observable

import java.util.*

interface IObservable<T> {
    fun subscribe(observer: (T) -> Unit)
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

class Observable<T>(private val source: Subject<T>) : IObservable<T> {
    override fun subscribe(observer: (T) -> Unit) {
        source.subscribe(observer)
    }

    override fun completed(handler: () -> Unit) {
        source.completed(handler)
    }
}
