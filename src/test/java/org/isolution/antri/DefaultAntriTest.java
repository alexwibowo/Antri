package org.isolution.antri;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
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

    @Test
    void queueing_lots_of_slow_tasks_on_one_queue() {
        final ConcurrentLinkedQueue<String> resultSink = new ConcurrentLinkedQueue<>();

        final int numMessages = 1_000;
        final int sleepDurationMillis = 10;

        enqueueTasks(resultSink, numMessages, sleepDurationMillis, 1);

        final int totalWaitMillis = numMessages * sleepDurationMillis;
        await().atMost(totalWaitMillis + 3_000, TimeUnit.MILLISECONDS).until(() -> Objects.equals(resultSink.size(), numMessages));
        for (int i = 0; i < numMessages; i++) {
            assertThat(resultSink).contains("message number " + i);
        }
    }

    @DisplayName("Test queueing tasks on Antri with multiple queues. Should complete faster than single queue.")
    @Test
    void queueing_lots_of_slow_tasks_on_multiple_queues() {
        final ConcurrentLinkedQueue<String> resultSink = new ConcurrentLinkedQueue<>();

        final int numberOfQueues = 10;
        final int numMessages = 1_000;
        final int sleepDurationMillis = 10;

        enqueueTasks(resultSink, numMessages, sleepDurationMillis, numberOfQueues);

        final int totalSingleThreadWaitMillis = numMessages * sleepDurationMillis;
        final int totalMultiThreadWaitMillis = totalSingleThreadWaitMillis / numberOfQueues;
        await().atMost(totalMultiThreadWaitMillis + 500, TimeUnit.MILLISECONDS).until(() -> Objects.equals(resultSink.size(), numMessages));
        for (int i = 0; i < numMessages; i++) {
            assertThat(resultSink).contains("message number " + i);
        }
    }

    private void enqueueTasks(ConcurrentLinkedQueue<String> resultSink, int numMessages, int sleepDurationMillis, int i2) {
        final CountDownLatch trigger = new CountDownLatch(1);
        final Antri antri = new DefaultAntri(i2);
        for (int i = 0; i < numMessages; i++) {
            final StringKey simpleKey = new StringKey("simpleKey" + i);
            final String message = "message number " + i;
            antri.execute(simpleKey, () -> {
                try {
                    trigger.await();
                    Thread.sleep(sleepDurationMillis);
                    resultSink.offer(message);
                } catch (InterruptedException e) {
                }
            });
        }

        trigger.countDown();
    }

}
