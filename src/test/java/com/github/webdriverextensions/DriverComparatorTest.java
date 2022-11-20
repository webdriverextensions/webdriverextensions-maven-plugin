package com.github.webdriverextensions;

import java.util.Arrays;
import java.util.Comparator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DriverComparatorTest {

    @Test
    void testById() {
        Comparator<Driver> uut = new DriverComparator.ById();
        Driver d1 = new Driver();
        Driver d2 = new Driver();
        Driver d3 = new Driver();
        d1.setCustomFileName("abc");
        d2.setCustomFileName("def");
        Driver[] actual = {null, d2, d3, d1};
        Driver[] expected = Arrays.copyOf(actual, actual.length);
        Arrays.sort(expected, uut);
        assertThat(expected).containsExactly(d1, d2, null, d3);
    }

    @Test
    void testByVersion() {
        Comparator<Driver> uut = new DriverComparator.ByVersion();
        Driver d1 = new Driver();
        Driver d2 = new Driver();
        Driver d3 = new Driver();
        d1.setVersion("1.2.3");
        d2.setVersion("1.2.4");
        Driver[] actual = {null, d2, d3, d1};
        Driver[] expected = Arrays.copyOf(actual, actual.length);
        Arrays.sort(expected, uut);
        assertThat(expected).containsExactly(d1, d2, null, d3);
    }

    @Test
    void testByArch() {
        Comparator<Driver> uut = new DriverComparator.ByArch();
        Driver d1 = new Driver();
        Driver d2 = new Driver();
        d1.setArch(Architecture.AMD64.toString());
        Driver[] actual = {null, d2, null, d1};
        Driver[] expected = Arrays.copyOf(actual, actual.length);
        Arrays.sort(expected, uut);
        assertThat(expected).containsExactly(d1, d2, null, null);
    }
}
