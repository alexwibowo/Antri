package org.isolution.antri;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class DefaultAntriTest {

    @Test
    void simple_test_with_one_queue() {
        final AtomicReference<String> resultSink = new AtomicReference<>();
        final Antri antri = new DefaultAntri(1);
        antri.execute(new StringKey("simpleKey"), () -> resultSink.set("Hello World"));
        await().atMost(120, TimeUnit.MILLISECONDS).until(() -> Objects.equals(resultSink.get(), "Hello World"));
    }

    @Test
    void queueing_multiple_tasks_on_one_queue() {
        final ConcurrentLinkedQueue<String> resultSink = new ConcurrentLinkedQueue<>();
        final Antri antri = new DefaultAntri(1);
        antri.execute(new StringKey("simpleKey"), () -> resultSink.offer("Hello there, sir"));
        antri.execute(new StringKey("anotherSimpleKey"), () -> resultSink.offer("Would you like a cup of tea?"));

        await().atMost(120, TimeUnit.MILLISECONDS).until(() -> Objects.equals(resultSink.size(), 2));
        assertThat(resultSink).contains("Hello there, sir", "Would you like a cup of tea?");
    }
}
