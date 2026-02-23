package com.example;

import java.util.function.UnaryOperator;

class ContextWriteSubscriber<T> extends CoreSubscriber<T> {
    private final Subscriber<T> downstream;
    private final Context modifiedContext;

    ContextWriteSubscriber(Subscriber<T> downstream, UnaryOperator<Context> contextModifier) {
        super(downstream);
        this.downstream = downstream;

        Context downstreamCtx = downstream.currentContext();
        this.modifiedContext = contextModifier.apply(downstreamCtx);
    }

    @Override
    public Context currentContext() {
        return modifiedContext;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        downstream.onSubscribe(subscription);
    }

    @Override
    public void onNext(T item) {
        downstream.onNext(item);
    }

    @Override
    public void onComplete() {
        downstream.onComplete();
    }
}
