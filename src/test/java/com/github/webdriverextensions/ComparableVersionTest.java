package com.github.webdriverextensions;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComparableVersionTest {

    @Test
    void testCompareTo() {
        assertThat(new ComparableVersion("2.45.0").compareTo(new ComparableVersion("2.46.0"))).isNegative();
        assertThat(new ComparableVersion("2.46.0").compareTo(new ComparableVersion("2.46.0"))).isZero();
        assertThat(new ComparableVersion("2.46.0").compareTo(new ComparableVersion("2.45.0"))).isPositive();

        assertThat(new ComparableVersion("2.46.0").compareTo(new ComparableVersion("70.0.3538.16"))).isNegative();
        assertThat(new ComparableVersion("70.0.3538.16").compareTo(new ComparableVersion("70.0.3538.16"))).isZero();
        assertThat(new ComparableVersion("70.0.3538.16").compareTo(new ComparableVersion("2.46.0"))).isPositive();

        assertThat(new ComparableVersion("70.0.3538.16").compareTo(new ComparableVersion("70.0.3538.67"))).isNegative();
        assertThat(new ComparableVersion("70.0.3538.67").compareTo(new ComparableVersion("70.0.3538.67"))).isZero();
        assertThat(new ComparableVersion("70.0.3538.67").compareTo(new ComparableVersion("70.0.3538.16"))).isPositive();
    }
}
