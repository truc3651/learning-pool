package example.com.monoflux;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * ═══════════════════════════════════════════════════════════════════
 * UNDERSTANDING REACTIVE STREAMS: Mono, Flux, and Streaming
 * ═══════════════════════════════════════════════════════════════════
 *
 * This file teaches reactive concepts using PURE JAVA — no libraries.
 * We build simplified versions of Mono and Flux from scratch so you
 * can understand what they REALLY are before using the real ones.
 *
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │              THE PROBLEM REACTIVE SOLVES                     │
 * │                                                              │
 * │  TRADITIONAL (Imperative):                                   │
 * │    String result = database.query("SELECT...");  // BLOCKS   │
 * │    // Thread is FROZEN here for 50ms                         │
 * │    // Then continues after data arrives                      │
 * │    process(result);                                          │
 * │                                                              │
 * │  REACTIVE:                                                   │
 * │    Mono<String> result = database.query("SELECT...");        │
 * │    // Returns IMMEDIATELY! No blocking.                      │
 * │    // 'result' is not the data itself — it's a DESCRIPTION   │
 * │    // of how to get the data.                                │
 * │    result.map(r -> process(r)).subscribe();                  │
 * │    // The thread is free to do other work!                   │
 * └──────────────────────────────────────────────────────────────┘
 *
 *
 * ════════════════════════════════════════════════════════════════
 * PART 1: WHAT IS A "STREAM" IN REACTIVE?
 * ════════════════════════════════════════════════════════════════
 *
 * The word "streaming" in reactive does NOT mean video streaming.
 * It means: DATA FLOWS THROUGH A PIPELINE, PIECE BY PIECE.
 *
 * Think about two ways to get water:
 *
 *   Way 1 (Traditional): Fill a bucket at the well, carry the
 *   entire bucket home, then use the water. You can't use ANY
 *   water until the ENTIRE bucket is full and carried home.
 *
 *   Way 2 (Streaming): Connect a pipe from the well to your
 *   house. Water flows continuously. You can start using water
 *   the moment the first drop arrives. You don't need to wait
 *   for all the water to arrive before using some.
 *
 * In programming terms:
 *
 *   Traditional (blocking):
 *     List<User> users = database.getAllUsers();  // Waits for ALL 10,000 users
 *     users.forEach(u -> sendEmail(u));           // Only NOW can we start
 *
 *   Streaming (reactive):
 *     Flux<User> users = database.getAllUsers();  // Returns immediately!
 *     users.flatMap(u -> sendEmail(u));           // Processes users AS THEY ARRIVE
 *     // User #1 gets their email while user #5000 is still being fetched!
 *
 *
 * ════════════════════════════════════════════════════════════════
 * PART 2: WHAT ARE Mono AND Flux?
 * ════════════════════════════════════════════════════════════════
 *
 * Mono and Flux are the two core types in Project Reactor
 * (the reactive library used by Spring WebFlux).
 *
 *   Mono<T>  = A stream that emits 0 or 1 item, then completes.
 *   Flux<T>  = A stream that emits 0 to N items, then completes.
 *
 * Think of them as CONTAINERS FOR FUTURE DATA:
 *
 *   Mono<User> = "I WILL give you one User... eventually."
 *   Flux<User> = "I WILL give you many Users... one at a time... eventually."
 *
 * Analogy:
 *   Mono = An Amazon package tracking notification.
 *          "Your package WILL arrive." You don't have it yet.
 *          You can plan what to do with it (map/flatMap)
 *          before it arrives. When it comes, your plan executes.
 *
 *   Flux = A Netflix subscription.
 *          New episodes come out one at a time, over time.
 *          You process (watch) each one as it arrives.
 *          You don't wait for the entire series to finish before
 *          watching episode 1.
 *
 * THE CRITICAL INSIGHT: Mono and Flux are LAZY.
 * Nothing happens until someone SUBSCRIBES.
 *
 *   Mono<User> user = userService.findById(1);  // NOTHING happens here!
 *   // The database has NOT been queried yet.
 *   // 'user' is just a description: "when someone subscribes,
 *   // query the database for user #1"
 *
 *   user.subscribe(u -> System.out.println(u));  // NOW the query runs!
 *
 * This is like writing a recipe vs. cooking. Creating a Mono/Flux
 * is writing the recipe. Subscribing is actually cooking.
 *
 *
 * ════════════════════════════════════════════════════════════════
 * PART 3: WHAT IS BACKPRESSURE?
 * ════════════════════════════════════════════════════════════════
 *
 * Backpressure is the most important concept in reactive streaming.
 *
 * Imagine a factory assembly line:
 *   - Station A makes widgets at 100/minute
 *   - Station B paints them at 10/minute
 *
 * Without backpressure: Station A floods Station B with unpainted
 * widgets. They pile up, memory fills up, things crash.
 *
 * With backpressure: Station B tells Station A: "Slow down!
 * I can only handle 10 per minute." Station A respects this.
 *
 * In reactive:
 *   Flux<Data> stream = database.streamAllRows();  // 100,000 rows/sec
 *   stream
 *     .map(row -> expensiveTransform(row))          // Can only do 1000/sec
 *     .subscribe();
 *   // Without backpressure: OutOfMemoryError
 *   // With backpressure (built into Flux): database automatically
 *   // slows down to match the transform speed.
 *
 *
 * ════════════════════════════════════════════════════════════════
 * PART 4: THE THREE SIGNALS
 * ════════════════════════════════════════════════════════════════
 *
 * Every reactive stream emits exactly three types of signals:
 *
 *   1. onNext(item)    — "Here's the next piece of data"
 *   2. onComplete()    — "I'm done, no more data"
 *   3. onError(error)  — "Something went wrong, I'm stopping"
 *
 * For a Mono<User>:
 *   onNext(user) → onComplete()          (found the user)
 *   onComplete()                          (user not found, empty Mono)
 *   onError(DatabaseException)            (query failed)
 *
 * For a Flux<User>:
 *   onNext(user1) → onNext(user2) → ... → onNext(userN) → onComplete()
 *   onNext(user1) → onError(ConnectionLost)   (failed mid-stream)
 *
 *
 * ════════════════════════════════════════════════════════════════
 * Now let's BUILD simplified versions of Mono and Flux to truly
 * understand what's happening inside.
 * ════════════════════════════════════════════════════════════════
 */
public class ReactiveFromScratch {

    // ══════════════════════════════════════════════════════════════
    // SIMPLIFIED MONO — A container for one future value
    // ══════════════════════════════════════════════════════════════

    /**
     * Our simplified Mono. In reality, Project Reactor's Mono is much more
     * complex, but this captures the essential idea.
     *
     * Think of SimpleMono as a "recipe card" — it describes HOW to get
     * a value, but doesn't actually get it until subscribe() is called.
     */
    static class SimpleMono<T> {
        private final Supplier<T> supplier;  // The "recipe" — how to get the value

        private SimpleMono(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        // Factory method: create a Mono that will emit one value
        static <T> SimpleMono<T> just(T value) {
            return new SimpleMono<>(() -> value);
        }

        // Factory method: create a Mono from a lazy computation
        static <T> SimpleMono<T> fromSupplier(Supplier<T> supplier) {
            return new SimpleMono<>(supplier);
        }

        // Factory method: empty Mono (emits nothing, just completes)
        static <T> SimpleMono<T> empty() {
            return new SimpleMono<>(() -> null);
        }

        /**
         * map() — Transform the value WITHOUT subscribing.
         *
         * This is like adding a step to the recipe:
         *   "After you get the value, apply this function to it."
         *
         * NOTHING EXECUTES YET. We're just building a plan.
         */
        <R> SimpleMono<R> map(Function<T, R> transformer) {
            return new SimpleMono<>(() -> {
                T value = this.supplier.get();
                return value != null ? transformer.apply(value) : null;
            });
        }

        /**
         * flatMap() — Transform the value into another Mono.
         *
         * Used when the transformation itself is async.
         * "After you get user, use their ID to fetch their orders."
         *
         *   mono.map(user -> user.getName())              // sync transform
         *   mono.flatMap(user -> fetchOrders(user.getId())) // async transform
         */
        <R> SimpleMono<R> flatMap(Function<T, SimpleMono<R>> transformer) {
            return new SimpleMono<>(() -> {
                T value = this.supplier.get();
                if (value != null) {
                    SimpleMono<R> nextMono = transformer.apply(value);
                    return nextMono.supplier.get();  // Subscribe to inner Mono
                }
                return null;
            });
        }

        /**
         * subscribe() — THIS IS WHERE EVERYTHING ACTUALLY HAPPENS.
         *
         * Calling subscribe is like saying "OK, execute the recipe NOW."
         * Before this call, nothing runs. After this call, the entire
         * chain of operations executes.
         */
        void subscribe(Consumer<T> onNext) {
            T value = supplier.get();  // Execute the "recipe"
            if (value != null) {
                onNext.accept(value);  // Deliver the result
            }
            // In real Reactor, onComplete() would be called here
        }
    }

    // ══════════════════════════════════════════════════════════════
    // SIMPLIFIED FLUX — A container for multiple future values
    // ══════════════════════════════════════════════════════════════

    /**
     * Our simplified Flux. Emits 0 to N items, one at a time.
     *
     * Think of SimpleFlux as a "conveyor belt recipe" — it describes
     * how to produce items one by one, but doesn't start producing
     * until subscribe() is called.
     */
    static class SimpleFlux<T> {
        private final Consumer<Consumer<T>> emitter;
        // ↑ This is the key: a function that, when called, will
        //   produce items and feed them to the consumer one at a time.

        private SimpleFlux(Consumer<Consumer<T>> emitter) {
            this.emitter = emitter;
        }

        // Create a Flux from specific items
        @SafeVarargs
        static <T> SimpleFlux<T> just(T... items) {
            return new SimpleFlux<>(consumer -> {
                for (T item : items) {
                    consumer.accept(item);  // Emit each item one by one
                }
            });
        }

        // Create a Flux from a list (simulates streaming from database)
        static <T> SimpleFlux<T> fromIterable(Iterable<T> items) {
            return new SimpleFlux<>(consumer -> {
                for (T item : items) {
                    consumer.accept(item);
                }
            });
        }

        // Create a Flux that emits items with a delay (simulates real streaming)
        static <T> SimpleFlux<T> fromStream(Iterable<T> items, long delayMs) {
            return new SimpleFlux<>(consumer -> {
                for (T item : items) {
                    try { Thread.sleep(delayMs); } catch (InterruptedException e) {}
                    consumer.accept(item);  // Each item arrives at different times!
                }
            });
        }

        /**
         * map() — Transform each item in the stream.
         * Like putting a machine on the conveyor belt that modifies each item.
         */
        <R> SimpleFlux<R> map(Function<T, R> transformer) {
            return new SimpleFlux<>(consumer -> {
                this.emitter.accept(item -> {
                    R transformed = transformer.apply(item);
                    consumer.accept(transformed);
                });
            });
        }

        /**
         * filter() — Only let certain items through.
         * Like a quality control inspector on the conveyor belt.
         */
        SimpleFlux<T> filter(java.util.function.Predicate<T> predicate) {
            return new SimpleFlux<>(consumer -> {
                this.emitter.accept(item -> {
                    if (predicate.test(item)) {
                        consumer.accept(item);
                    }
                });
            });
        }

        /**
         * subscribe() — Start the conveyor belt!
         * Nothing produces until this is called.
         */
        void subscribe(Consumer<T> onNext) {
            emitter.accept(onNext);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DEMONSTRATIONS
    // ══════════════════════════════════════════════════════════════

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("  DEMO 1: Mono — Lazy single-value computation");
        System.out.println("═══════════════════════════════════════════════\n");

        // Create a Mono — NOTHING executes yet!
        SimpleMono<String> userMono = SimpleMono.fromSupplier(() -> {
            System.out.println("  [Mono] Querying database for user...");
            // In real code, this would be a non-blocking DB call
            return "Alice";
        });

        System.out.println("1. Mono created. Has the database been queried? NO!");
        System.out.println("   The Mono is just a PLAN, not a result.\n");

        // Add transformations — still nothing executes!
        SimpleMono<String> greetingMono = userMono
                .map(name -> {
                    System.out.println("  [map] Transforming name to greeting...");
                    return "Hello, " + name + "!";
                })
                .map(greeting -> {
                    System.out.println("  [map] Adding emoji...");
                    return greeting + " 👋";
                });

        System.out.println("2. Added two map() transforms. Has anything executed? Still NO!");
        System.out.println("   We've just added steps to the recipe.\n");

        // NOW subscribe — everything executes!
        System.out.println("3. Calling subscribe() — NOW everything runs:");
        greetingMono.subscribe(result -> {
            System.out.println("  [subscribe] Final result: " + result);
        });

        System.out.println("\n");

        // ─────────────────────────────────────────────────────────────

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("  DEMO 2: Flux — Streaming multiple values");
        System.out.println("═══════════════════════════════════════════════\n");

        // Create a Flux of numbers — nothing happens yet
        SimpleFlux<Integer> numbers = SimpleFlux.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        System.out.println("Processing numbers through a reactive pipeline:");
        System.out.println("  Source: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]");
        System.out.println("  Filter: keep only even numbers");
        System.out.println("  Map: multiply by 10");
        System.out.println("  Result:\n");

        // Build a pipeline — still lazy!
        numbers
                .filter(n -> n % 2 == 0)       // Keep even numbers: 2, 4, 6, 8, 10
                .map(n -> n * 10)               // Multiply by 10: 20, 40, 60, 80, 100
                .subscribe(n -> System.out.println("    Received: " + n));

        System.out.println("\n");

        // ─────────────────────────────────────────────────────────────

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("  DEMO 3: Streaming — Items arrive over TIME");
        System.out.println("═══════════════════════════════════════════════\n");

        System.out.println("Simulating a database that returns rows one by one:");
        System.out.println("(Watch the timestamps — each item arrives separately!)\n");

        List<String> dbRows = List.of(
                "User: Alice, age 30",
                "User: Bob, age 25",
                "User: Charlie, age 35"
        );

        long startTime = System.currentTimeMillis();

        // This Flux emits items with a delay — simulating real streaming
        SimpleFlux.fromStream(dbRows, 500)  // 500ms between items
                .map(row -> row.toUpperCase())
                .subscribe(row -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    System.out.println("    [" + elapsed + "ms] Received: " + row);
                    // KEY INSIGHT: We process each row AS IT ARRIVES.
                    // We don't wait for all 3 rows before starting!
                });

        System.out.println("\n");

        // ─────────────────────────────────────────────────────────────

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("  DEMO 4: flatMap — Chaining async operations");
        System.out.println("═══════════════════════════════════════════════\n");

        System.out.println("Scenario: Find user → then fetch their orders\n");

        // Simulate: findUser(1) returns Mono<User>
        SimpleMono<String> user = SimpleMono.fromSupplier(() -> {
            System.out.println("  [Step 1] Finding user #1 in database...");
            return "Alice";
        });

        // flatMap chains async operations:
        // "After you get the user, use their name to fetch orders"
        SimpleMono<String> orderInfo = user.flatMap(userName -> {
            System.out.println("  [Step 2] Found " + userName + "! Now fetching orders...");
            // This returns ANOTHER Mono — hence flatMap, not map
            return SimpleMono.fromSupplier(() -> {
                return userName + "'s orders: [Laptop, Phone, Headphones]";
            });
        });

        System.out.println("Pipeline built. Nothing executed yet.\n");
        System.out.println("Subscribing now:");
        orderInfo.subscribe(result -> {
            System.out.println("  [Result] " + result);
        });

        System.out.println("\n");

        // ─────────────────────────────────────────────────────────────

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("  DEMO 5: Traditional vs Reactive Comparison");
        System.out.println("═══════════════════════════════════════════════\n");

        System.out.println("""
            ┌────────────────────────────────────────────────────┐
            │  TRADITIONAL (Blocking)                            │
            │                                                    │
            │  User user = userRepo.findById(1);     // 50ms ⏳ │
            │  List<Order> orders = orderRepo         // 30ms ⏳ │
            │      .findByUserId(user.getId());                  │
            │  Address addr = addressRepo             // 20ms ⏳ │
            │      .findByUserId(user.getId());                  │
            │                                                    │
            │  Total: 100ms, thread blocked the ENTIRE time     │
            │  Thread can do NOTHING else during those 100ms     │
            └────────────────────────────────────────────────────┘
            
            ┌────────────────────────────────────────────────────┐
            │  REACTIVE (Non-blocking)                           │
            │                                                    │
            │  Mono<User> user = userRepo.findById(1);           │
            │  Mono<Tuple> result = user.flatMap(u ->            │
            │      Mono.zip(                                     │
            │          orderRepo.findByUserId(u.getId()),  // ┐  │
            │          addressRepo.findByUserId(u.getId()) // ├ PARALLEL!
            │      )                                       // ┘  │
            │  );                                                │
            │                                                    │
            │  Total: 50ms + 30ms = 80ms (orders & address      │
            │         fetched in PARALLEL after user loads!)     │
            │  Thread is NEVER blocked — free to serve others    │
            └────────────────────────────────────────────────────┘
            
            ┌────────────────────────────────────────────────────┐
            │  KEY TAKEAWAY:                                     │
            │                                                    │
            │  Mono/Flux are not just "another way to do the     │
            │  same thing." They enable:                         │
            │                                                    │
            │  1. NON-BLOCKING: Thread never waits for I/O       │
            │  2. COMPOSABLE: Chain operations like LEGO blocks  │
            │  3. PARALLEL: Run independent operations at once   │
            │  4. STREAMING: Process data as it arrives          │
            │  5. BACKPRESSURE: Consumer controls the speed      │
            │  6. LAZY: Nothing runs until you subscribe         │
            └────────────────────────────────────────────────────┘
        """);
    }
}