package com.example;

class FluxJust<T> implements Publisher<T> {
    private final T[] items;

    @SafeVarargs
    FluxJust(T... items) {
        this.items = items;
    }

    @Override
    public void subscribe(Subscriber<T> subscriber) {
        // Map call this method
        // this method call Map's onSubscribe
        // subscriber = MapSubscriber

        Context ctx = subscriber.currentContext();
        String traceId = ctx.getOrDefault("traceId", "no-trace");
        System.out.println("FluxJust: traceId = " + traceId);
        String userId = ctx.getOrDefault("userId", "no-userId");
        System.out.println("FluxJust: userId = " + userId);

        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                for (T item : items) {
                    subscriber.onNext(item);
                }
                subscriber.onComplete();
            }
        });
    }
}
