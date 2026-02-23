package com.example;

interface Publisher<T> {
    void subscribe(Subscriber<T> subscriber);
}
