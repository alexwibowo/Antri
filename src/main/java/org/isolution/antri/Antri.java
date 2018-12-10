package org.isolution.antri;

import org.jetbrains.annotations.NotNull;

public interface Antri {
    void execute(@NotNull Key key,
                 @NotNull Runnable task);

    void stop();

    /**
     * Key <strong>must</strong> implements {@link #hashCode()}
     */
    interface Key {
    }
}
