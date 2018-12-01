package io.github.janmalch.sacajawea.observable

import java.util.*

interface IObserver<T> {
    fun update(t: T)
}

open class Observable<T> {

    private val observers = ArrayList<(T) -> Unit>()
    private val completions = ArrayList<() -> Unit>()

    fun subscribe(observer: (T) -> Unit) {
        observers.add(observer)
    }

    fun completed(handler: () -> Unit) {
        completions.add(handler)
    }

    fun unsubscribe(observer: (T) -> Unit) {
        observers.remove(observer)
    }

    fun complete() {
        for (fn in completions) {
            fn()
        }
        observers.clear()
    }

    fun next(t: T) {
        for (observer in observers) {
            observer(t)
        }
    }
}

/*
class Observable<T> {

    private val observers = ArrayList<IObserver<T>>()

    fun subscribe(observer: IObserver<T>) {
        observers.add(observer)
    }

    fun unsubscribe(observer: IObserver<T>) {
        observers.remove(observer)
    }

    fun complete() {
        observers.clear()
    }

    fun next(t: T) {
        for (observer in observers) {
            observer.update(t)
        }
    }
}*/
