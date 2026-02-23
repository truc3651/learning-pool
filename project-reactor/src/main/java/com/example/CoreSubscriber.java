package com.example;

abstract class CoreSubscriber<T> implements Subscriber<T> {
    protected final Subscriber<?> downstream;

    CoreSubscriber(Subscriber<?> downstream) {
        this.downstream = downstream;
    }

    @Override
    public Context currentContext() {
        return downstream.currentContext();
    }
}