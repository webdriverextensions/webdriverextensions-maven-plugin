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

class DriverComparator {

    private DriverComparator() {
    }

    static class ById implements Comparator<Driver> {

        @Override
        public int compare(Driver o1, Driver o2) {
            if (o1 == null || o1.getId() == null) {
                return o2 == null || o2.getId() == null ? 0 : 1;
            }
            if (o2 == null || o2.getId() == null) {
                return -1;
            }
            return o1.getId().compareTo(o2.getId());
        }
    }

    static class ByVersion implements Comparator<Driver> {

        @Override
        public int compare(Driver o1, Driver o2) {
            if (o1 == null || o1.getVersion() == null) {
                return o2 == null || o2.getVersion() == null ? 0 : 1;
            }
            if (o2 == null || o2.getVersion() == null) {
                return -1;
            }
            return o1.getComparableVersion().compareTo(o2.getComparableVersion());
        }
    }
}
