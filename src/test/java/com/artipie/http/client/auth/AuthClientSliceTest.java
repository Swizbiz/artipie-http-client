/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
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
package com.artipie.http.client.auth;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AuthClientSlice}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class AuthClientSliceTest {

    @Test
    void shouldAuthenticateFirstRequestWithEmptyHeadersFirst() {
        final FakeAuthenticator fake = new FakeAuthenticator(Headers.EMPTY);
        new AuthClientSlice(
            (line, headers, body) -> StandardRs.EMPTY,
            fake
        ).response(
            new RequestLine(RqMethod.GET, "/").toString(),
            new Headers.From("X-Header", "The Value"),
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            fake.capture(0),
            new IsEqual<>(Headers.EMPTY)
        );
    }

    @Test
    void shouldAuthenticateOnceIfNotUnauthorized() {
        final AtomicReference<Iterable<Map.Entry<String, String>>> capture;
        capture = new AtomicReference<>();
        final Header original = new Header("Original", "Value");
        final Authorization.Basic auth = new Authorization.Basic("me", "pass");
        new AuthClientSlice(
            (line, headers, body) -> {
                capture.set(headers);
                return StandardRs.EMPTY;
            },
            new FakeAuthenticator(new Headers.From(auth))
        ).response(
            new RequestLine(RqMethod.GET, "/resource").toString(),
            new Headers.From(original),
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            capture.get(),
            Matchers.containsInAnyOrder(original, auth)
        );
    }

    @Test
    void shouldAuthenticateWithHeadersIfUnauthorized() {
        final Header rsheader = new Header("Abc", "Def");
        final FakeAuthenticator fake = new FakeAuthenticator(Headers.EMPTY, Headers.EMPTY);
        new AuthClientSlice(
            (line, headers, body) -> new RsWithHeaders(
                new RsWithStatus(RsStatus.UNAUTHORIZED),
                new Headers.From(rsheader)
            ),
            fake
        ).response(
            new RequestLine(RqMethod.GET, "/foo/bar").toString(),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            fake.capture(1),
            Matchers.containsInAnyOrder(rsheader)
        );
    }

    @Test
    void shouldAuthenticateOnceIfUnauthorizedButAnonymous() {
        final AtomicInteger capture = new AtomicInteger();
        new AuthClientSlice(
            (line, headers, body) -> {
                capture.incrementAndGet();
                return new RsWithStatus(RsStatus.UNAUTHORIZED);
            },
            Authenticator.ANONYMOUS
        ).response(
            new RequestLine(RqMethod.GET, "/secret/resource").toString(),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            capture.get(),
            new IsEqual<>(1)
        );
    }

    @Test
    void shouldAuthenticateTwiceIfNotUnauthorized() {
        final AtomicReference<Iterable<Map.Entry<String, String>>> capture;
        capture = new AtomicReference<>();
        final Header original = new Header("RequestHeader", "Original Value");
        final Authorization.Basic auth = new Authorization.Basic("user", "password");
        new AuthClientSlice(
            (line, headers, body) -> {
                capture.set(headers);
                return new RsWithStatus(RsStatus.UNAUTHORIZED);
            },
            new FakeAuthenticator(Headers.EMPTY, new Headers.From(auth))
        ).response(
            new RequestLine(RqMethod.GET, "/top/secret").toString(),
            new Headers.From(original),
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            capture.get(),
            Matchers.containsInAnyOrder(original, auth)
        );
    }

    @Test
    void shouldNotCompleteOriginSentWhenAuthSentNotComplete() {
        final AtomicReference<CompletionStage<Void>> capture = new AtomicReference<>();
        new AuthClientSlice(
            (line, headers, body) -> connection -> {
                final CompletionStage<Void> sent = StandardRs.EMPTY.send(connection);
                capture.set(sent);
                return sent;
            },
            new FakeAuthenticator(Headers.EMPTY)
        ).response(
            new RequestLine(RqMethod.GET, "/path").toString(),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, headers, body) -> new CompletableFuture<>()
        );
        Assertions.assertThrows(
            TimeoutException.class,
            () -> {
                final int timeout = 500;
                capture.get().toCompletableFuture().get(timeout, TimeUnit.MILLISECONDS);
            }
        );
    }

    /**
     * Fake authenticator providing specified results
     * and capturing `authenticate()` method arguments.
     *
     * @since 0.3
     */
    private static final class FakeAuthenticator implements Authenticator {

        /**
         * Results `authenticate()` method should return by number of invocation.
         */
        private final List<Headers> results;

        /**
         * Captured `authenticate()` method arguments by number of invocation..
         */
        private final AtomicReference<List<Headers>> captures;

        private FakeAuthenticator(final Headers... results) {
            this(Arrays.asList(results));
        }

        private FakeAuthenticator(final List<Headers> results) {
            this.results = results;
            this.captures = new AtomicReference<>(Collections.emptyList());
        }

        public Headers capture(final int index) {
            return this.captures.get().get(index);
        }

        @Override
        public Headers authenticate(final Headers headers) {
            final List<Headers> prev = this.captures.get();
            final List<Headers> updated = new ArrayList<>(prev);
            updated.add(headers);
            this.captures.set(updated);
            return this.results.get(prev.size());
        }
    }
}
