package com.example;

import java.util.function.Predicate;

class FluxFilter<T> implements Publisher<T> {
    private final Publisher<T> upstream;
    private final Predicate<T> predicate;

    FluxFilter(Publisher<T> upstream, Predicate<T> predicate) {
        this.upstream = upstream;
        this.predicate = predicate;
    }

    @Override
    public void subscribe(Subscriber<T> downstreamSubscriber) {
        FilterSubscriber<T> filterSubscriber = new FilterSubscriber<>(downstreamSubscriber, predicate);
        upstream.subscribe(filterSubscriber);
    }
}

class FilterSubscriber<T> extends CoreSubscriber<T> {
    private final Subscriber<T> downstream;
    private final Predicate<T> predicate;

    FilterSubscriber(Subscriber<T> downstream, Predicate<T> predicate) {
        super(downstream);
        this.downstream = downstream;
        this.predicate = predicate;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        downstream.onSubscribe(subscription);
    }

    @Override
    public void onNext(T item) {
        if (predicate.test(item)) {
            downstream.onNext(item);
        }
    }

    @Override
    public void onComplete() {
        downstream.onComplete();
    }
}
