package org.isolution.antri;

import net.openhft.chronicle.threads.LongPauser;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class DefaultAntri implements Antri {
    private final SingleAntri[] queues;
    private final ThreadPoolExecutor executor;
    private final AtomicInteger threadIndexCounter = new AtomicInteger(0);
    private final LongPauser pauser;

    public DefaultAntri(final int numberOfQueues){
        queues = new SingleAntri[numberOfQueues];
        for (int i = 0; i < numberOfQueues; i++) {
            queues[i] = new SingleAntri();
        }
        pauser = new LongPauser(100, 100, 10, 100_000, TimeUnit.MICROSECONDS);

        // create threads, where each thread is responsible to look after one SingleAntri
        executor = new ThreadPoolExecutor(
                numberOfQueues, numberOfQueues,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> new WorkerThread(pauser, threadIndexCounter.getAndIncrement())
        );
        executor.prestartAllCoreThreads();
    }

    @Override
    public void execute(@NotNull final Key key,
                        @NotNull final Runnable task) {
        int queueIndex = key.hashCode() % queues.length;
        queues[queueIndex].enlist(task);
    }

    @Override
    public void stop() {
        executor.shutdown();
    }


    class WorkerThread extends Thread {
        private final LongPauser pauser;
        private final int queueIndex;

        WorkerThread(final LongPauser pauser,
                     final int index) {
            this.pauser = pauser;
            this.queueIndex = index;
        }

        @Override
        public void run() {
            final Runnable nextTask = queues[queueIndex].next();

            if (nextTask != null) {
                nextTask.run();
                pauser.reset();
            }else{
                pauser.pause();
            }
        }
    }


    private static class SingleAntri extends Thread{
        private final ConcurrentLinkedQueue<Runnable> tasks;

        private SingleAntri() {
            this.tasks = new ConcurrentLinkedQueue<>();
        }

        void enlist(final Runnable task) {
            tasks.offer(task);
        }

        Runnable next() {
            return tasks.poll();
        }
    }
}
