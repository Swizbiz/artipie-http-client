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
package com.artipie.http.client.jetty;

import com.artipie.http.Headers;
import com.artipie.http.client.HttpServer;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsWithBody;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests checking for leaks in {@link JettyClientSlice}.
 *
 * @since 0.1
 */
final class JettyClientSliceLeakTest {

    /**
     * HTTP server used in tests.
     */
    private final HttpServer server = new HttpServer();

    /**
     * HTTP client used in tests.
     */
    private final HttpClient client = new HttpClient();

    /**
     * HTTP client sliced being tested.
     */
    private JettyClientSlice slice;

    @BeforeEach
    void setUp() throws Exception {
        this.server.update(
            (line, headers, body) -> new RsWithBody(
                Flowable.just(ByteBuffer.wrap("data".getBytes()))
            )
        );
        final int port = this.server.start();
        this.client.start();
        this.slice = new JettyClientSlice(this.client, false, "localhost", port);
    }

    @AfterEach
    void tearDown() throws Exception {
        this.server.stop();
        this.client.stop();
    }

    @Test
    void shouldNotLeakConnectionsIfBodyNotRead() throws Exception {
        final int total = 1025;
        for (int count = 0; count < total; count += 1) {
            this.slice.response(
                new RequestLine(RqMethod.GET, "/").toString(),
                Headers.EMPTY,
                Flowable.empty()
            ).send(
                (status, headers, body) -> CompletableFuture.allOf()
            ).toCompletableFuture().get(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldNotLeakConnectionsIfSendFails() throws Exception {
        final int total = 1025;
        for (int count = 0; count < total; count += 1) {
            final CompletionStage<Void> sent = this.slice.response(
                new RequestLine(RqMethod.GET, "/").toString(),
                Headers.EMPTY,
                Flowable.empty()
            ).send(
                (status, headers, body) -> {
                    final CompletableFuture<Void> future = new CompletableFuture<>();
                    future.completeExceptionally(new IllegalStateException());
                    return future;
                }
            );
            try {
                sent.toCompletableFuture().get(2, TimeUnit.SECONDS);
            } catch (final ExecutionException expected) {
            }
        }
    }
}
