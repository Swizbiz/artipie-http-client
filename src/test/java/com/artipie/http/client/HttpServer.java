/*
 * MIT License
 *
 * Copyright (c) 2020-2022 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.http.client;

import com.artipie.http.Slice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.Vertx;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HTTP server with dynamically controlled behavior for usage in tests.
 *
 * @since 0.1
 * @todo #23:30min Create and use JUnit extension for `HttpServer` management.
 *  Every test suite is required to create `HttpServer`, start it before each test,
 *  memorize instance and stop after test is finished.
 *  This logic may be extracted to JUnit extension, so it won't be duplicated in every test class.
 */
public class HttpServer {

    /**
     * Vert.x instance used for test server.
     */
    private Vertx vertx;

    /**
     * Vert.x used to serve HTTP request.
     */
    private VertxSliceServer server;

    /**
     * Listened port.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private int port;

    /**
     * Reference to handler slice used in test.
     */
    private final AtomicReference<Slice> handler = new AtomicReference<>();

    /**
     * Start the server.
     *
     * @return Listened port.
     */
    public int start() {
        return this.start(new HttpServerOptions().setPort(0));
    }

    /**
     * Start the server.
     * @param options Options to use.
     * @return Listened port.
     */
    public int start(final HttpServerOptions options) {
        this.vertx = Vertx.vertx();
        this.server = new VertxSliceServer(
            this.vertx,
            (line, headers, body) -> this.handler.get().response(line, headers, body),
            options
        );
        this.port = this.server.start();
        return this.port;
    }

    /**
     * Get port the servers listens on.
     *
     * @return Listened port.
     */
    public int port() {
        return this.port;
    }

    /**
     * Stop the server releasing all resources.
     */
    public void stop() {
        this.server.close();
        this.vertx.close();
    }

    /**
     * Update handler slice.
     *
     * @param value Handler slice.
     */
    public void update(final Slice value) {
        this.handler.set(value);
    }
}
