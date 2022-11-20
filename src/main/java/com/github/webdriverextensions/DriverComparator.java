/*
 * Copyright 2021 WebDriver Extensions.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.webdriverextensions;

import java.util.Comparator;

import static org.codehaus.plexus.util.StringUtils.isBlank;


class DriverComparator {

    private DriverComparator() {
    }

    static class ById implements Comparator<Driver> {

        @Override
        public int compare(Driver o1, Driver o2) {
            if (o1 == null || isBlank(o1.getId()) || "null".equals(o1.getId())) {
                return (o2 == null || isBlank(o2.getId()) || "null".equals(o2.getId())) ? 0 : 1;
            } else if (o2 == null || isBlank(o2.getId()) || "null".equals(o2.getId())) {
                return -1;
            } else {
                return o1.getId().compareToIgnoreCase(o2.getId());
            }
        }
    }

    static class ByVersion implements Comparator<Driver> {

        @Override
        public int compare(Driver o1, Driver o2) {
            if (o1 == null || isBlank(o1.getVersion())) {
                return o2 == null || isBlank(o2.getVersion()) ? 0 : 1;
            } else if (o2 == null || isBlank(o2.getVersion())) {
                return -1;
            } else {
                return o1.getComparableVersion().compareTo(o2.getComparableVersion());
            }
        }
    }

    /**
     * sort by {@link Architecture} and their {@code ordinal} with {@code null} last
     */
    static class ByArch implements Comparator<Driver> {

        @Override
        public int compare(Driver o1, Driver o2) {
            if (o1 == null) {
                return o2 == null ? 0 : 1;
            } else if (o2 == null) {
                return -1;
            } else {
                return o1.getArchitecture().compareTo(o2.getArchitecture());
            }
        }
    }
}
