package com.example;

class MySubscriber<T> implements Subscriber<T> {
    private final Context initialContext;

    MySubscriber(Context initialContext) {
        this.initialContext = initialContext;
    }

    MySubscriber() {
        this(Context.EMPTY);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        System.out.println("[RESULT] MySubscriber received: " + item);
    }

    @Override
    public void onComplete() {
        System.out.println("[RESULT] MySubscriber: onComplete()");
    }

    @Override
    public Context currentContext() {
        return initialContext;
    }
}
