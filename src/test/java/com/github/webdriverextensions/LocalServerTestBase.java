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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.testing.classic.ClassicTestServer;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

abstract class LocalServerTestBase {

    protected ClassicTestServer server;

    @Rule
    public final ExternalResource serverResource = new ExternalResource() {

        @Override
        protected void before() throws Throwable {
            server = new ClassicTestServer();
        }

        @Override
        protected void after() {
            if (server != null) {
                try {
                    server.shutdown(CloseMode.IMMEDIATE);
                    server = null;
                } catch (final Exception ignore) {
                }
            }
        }

    };

    protected void start() throws IOException {
        server.start();
    }

    protected URL getCompleteUrlFor(String urlPart) throws MalformedURLException {
        if (server != null) {
            return new URL(String.format("http://localhost:%d%s", server.getPort(), urlPart));
        }
        throw new IllegalStateException("server not started yet. call start() before using this method.");
    }
}
