package org.isolution.antri;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class StringKey implements Antri.Key {
    private final String key;

    public StringKey(final @NotNull String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringKey stringKey = (StringKey) o;
        return Objects.equals(key, stringKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "StringKey{" +
                "key='" + key + '\'' +
                '}';
    }
}
