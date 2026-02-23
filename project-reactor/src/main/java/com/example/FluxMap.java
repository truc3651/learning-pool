package com.example;

import java.util.function.Function;

class FluxMap<T, R> implements Publisher<R> {
    private final Publisher<T> upstream;
    private final Function<T, R> mapper;

    FluxMap(Publisher<T> upstream, Function<T, R> mapper) {
        this.upstream = upstream;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Subscriber<R> downstreamSubscriber) {
        // Filter call this method
        MapSubscriber<T, R> mapSubscriber = new MapSubscriber<>(downstreamSubscriber, mapper);
        upstream.subscribe(mapSubscriber);
    }
}

class MapSubscriber<T, R> extends CoreSubscriber<T> {
    private final Subscriber<R> downstream;
    private final Function<T, R> mapper;

    MapSubscriber(Subscriber<R> downstream, Function<T, R> mapper) {
        super(downstream);
        this.downstream = downstream;
        this.mapper = mapper;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        downstream.onSubscribe(subscription);
    }

    @Override
    public void onNext(T item) {
        Context ctx = currentContext();
        System.out.println("FluxMap: ctx = " + ctx);

        R mapped = mapper.apply(item);
        downstream.onNext(mapped);
    }

    @Override
    public void onComplete() {
        downstream.onComplete();
    }
}