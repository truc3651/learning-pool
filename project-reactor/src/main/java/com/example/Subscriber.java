package com.example;

interface Subscriber<T> {
    void onSubscribe(Subscription subscription);
    void onNext(T item);
    void onComplete();

    Context currentContext();
}
