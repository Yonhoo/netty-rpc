
package com.yonhoo.nettyrpc.common;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class ExecutorUtil {
    public static boolean isTerminated(Executor executor) {
        if (executor instanceof ExecutorService) {
            return ((ExecutorService) executor).isTerminated();
        }
        return false;
    }

    /**
     * Use the shutdown pattern from:
     * https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
     *
     * @param executor the Executor to shut down
     * @param timeout  the timeout in milliseconds before termination
     */
    public static void gracefulShutdown(Executor executor, long timeout) {
        if (!(executor instanceof ExecutorService) || isTerminated(executor)) {
            return;
        }
        final ExecutorService es = (ExecutorService) executor;
        try {
            // Disable new tasks from being submitted
            es.shutdown();
        } catch (SecurityException | NullPointerException ex2) {
            return;
        }
        try {
            // Wait a while for existing tasks to terminate
            if (!es.awaitTermination(timeout, TimeUnit.SECONDS)) {
                es.shutdownNow();
            }
        } catch (InterruptedException ex) {
            es.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void shutdownNow(Executor executor, final int timeout) {
        if (!(executor instanceof ExecutorService) || isTerminated(executor)) {
            return;
        }
        final ExecutorService es = (ExecutorService) executor;
        try {
            es.shutdownNow();
        } catch (SecurityException | NullPointerException ex2) {
            return;
        }
        try {
            es.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

}
