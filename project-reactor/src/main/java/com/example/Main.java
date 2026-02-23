package com.example;

public class Main {
    public static void main(String[] args) {
        Publisher<String> step1 = new FluxJust<>("a", "b", "c");
        Publisher<String> step2 = new FluxMap<>(step1, String::toUpperCase);
        Publisher<String> step3 = new FluxFilter<>(step2, s -> !s.equals("B"));

        Publisher<String> step4 = new FluxContextWrite<>(step3,
                ctx -> ctx.put("traceId", "abc-123"));
        Publisher<String> pipeline = new FluxContextWrite<>(step4,
                ctx -> ctx.put("userId", "user-42"));

        pipeline.subscribe(new MySubscriber<>());
    }
}
