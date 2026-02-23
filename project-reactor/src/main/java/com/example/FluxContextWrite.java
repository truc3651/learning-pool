package com.example;

import java.util.function.UnaryOperator;

class FluxContextWrite<T> implements Publisher<T> {
    private final Publisher<T> upstream;
    private final UnaryOperator<Context> contextModifier;

    FluxContextWrite(Publisher<T> upstream, UnaryOperator<Context> contextModifier) {
        this.upstream = upstream;
        this.contextModifier = contextModifier;
    }

    @Override
    public void subscribe(Subscriber<T> downstreamSubscriber) {
        ContextWriteSubscriber<T> contextSubscriber =
                new ContextWriteSubscriber<>(downstreamSubscriber, contextModifier);
        upstream.subscribe(contextSubscriber);
    }
}
