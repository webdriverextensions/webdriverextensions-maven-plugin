package com.github.webdriverextensions;

import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

/**
 * Reflects possible and known values of the Java system property {@code os.arch}.
 * 
 * @see <a href="https://github.com/openjdk/jdk/blob/jdk-20+15/src/java.base/windows/native/libjava/java_props_md.c#L580">the definition of {@code os.arch} values for Windows</a>
 * @see <a href="https://github.com/openjdk/jdk/blob/jdk-20+15/src/java.base/unix/native/libjava/java_props_md.c#L419">the definition of {@code os.arch} values for Linux</a>
 */
@RequiredArgsConstructor
public enum Architecture {
    /**
     * {@code os.arch "x86"}
     */
    X86("x86"),
    /**
     * {@code os.arch "amd64"}
     */
    AMD64("amd64"),
    /**
     * {@code os.arch "aarch64"}
     */
    ARM64("aarch64"),
    /**
     * {@code os.arch "ia64"}
     */
    IA64("ia64"),
    /**
     * {@code os.arch "unknown"}
     */
    UNKNOWN("unknown");

    private final String archName;

    @Nonnull
    static Architecture extractFromSysProperty() {
        return fromArchName(System.getProperty("os.arch", UNKNOWN.archName));
    }

    @Nonnull
    public static Architecture fromArchName(@Nullable String archName) {
        return Stream.of(values()).filter(value -> value.archName.equalsIgnoreCase(archName))
                .findAny().orElse(UNKNOWN);
    }

    @Override
    public String toString() {
        return archName;
    }
}
