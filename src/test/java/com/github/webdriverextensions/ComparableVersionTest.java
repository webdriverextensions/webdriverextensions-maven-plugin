package com.github.webdriverextensions;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ComparableVersionTest {

    @Test
    public void testConstructor() {
        assertThat(new ComparableVersion("2.45.0").compareTo(new ComparableVersion("2.46.0")), is(-1));
        assertThat(new ComparableVersion("2.46.0").compareTo(new ComparableVersion("2.46.0")), is(0));
        assertThat(new ComparableVersion("2.46.0").compareTo(new ComparableVersion("2.45.0")), is(1));

        assertThat(new ComparableVersion("2.46.0").compareTo(new ComparableVersion("70.0.3538.16")), is(-1));
        assertThat(new ComparableVersion("70.0.3538.16").compareTo(new ComparableVersion("70.0.3538.16")), is(0));
        assertThat(new ComparableVersion("70.0.3538.16").compareTo(new ComparableVersion("2.46.0")), is(1));

        assertThat(new ComparableVersion("70.0.3538.16").compareTo(new ComparableVersion("70.0.3538.67")), is(-1));
        assertThat(new ComparableVersion("70.0.3538.67").compareTo(new ComparableVersion("70.0.3538.67")), is(0));
        assertThat(new ComparableVersion("70.0.3538.67").compareTo(new ComparableVersion("70.0.3538.16")), is(1));
    }
}
