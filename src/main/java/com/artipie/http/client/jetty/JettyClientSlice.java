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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.client.misc.PublisherAs;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.reactivestreams.Publisher;

/**
 * ClientSlices implementation using Jetty HTTP client as back-end.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class JettyClientSlice implements Slice {

    /**
     * HTTP client.
     */
    private final HttpClient client;

    /**
     * Secure connection flag.
     */
    private final boolean secure;

    /**
     * Host name.
     */
    private final String host;

    /**
     * Port.
     */
    private final int port;

    /**
     * Ctor.
     *
     * @param client HTTP client.
     * @param secure Secure connection flag.
     * @param host Host name.
     * @param port Port.
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    JettyClientSlice(
        final HttpClient client,
        final boolean secure,
        final String host,
        final int port
    ) {
        this.client = client;
        this.secure = secure;
        this.host = host;
        this.port = port;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            this.request(line, headers, body).thenCompose(
                request -> Flowable.fromPublisher(
                    ReactiveRequest.newBuilder(request).build().response(
                        (response, rsbody) -> Flowable.just(
                            (Response) connection -> {
                                final ClosablePublisher closable = new ClosablePublisher(rsbody);
                                final RsFull origin = new RsFull(
                                    new RsStatus.ByCode(response.getStatus()).find(),
                                    new ResponseHeaders(response),
                                    Flowable.fromPublisher(closable).map(chunk -> chunk.buffer)
                                );
                                return origin.send(connection).handle(
                                    (nothing, throwable) -> {
                                        final CompletableFuture<Void> original;
                                        if (throwable == null) {
                                            original = CompletableFuture.allOf();
                                        } else {
                                            original = new CompletableFuture<>();
                                            original.completeExceptionally(throwable);
                                        }
                                        return closable.close().thenCompose(nthng -> original);
                                    }
                                ).thenCompose(Function.identity());
                            }
                        )
                    )
                ).singleOrError().to(SingleInterop.get())
            )
        );
    }

    /**
     * Create request.
     *
     * @param line Request line.
     * @param headers Request headers.
     * @param body Request body.
     * @return Request built from parameters.
     * @todo #1:30min Send request body in reactive way.
     *  `JettyClientSlice` reads whole request body before sending. It is inefficient
     *  for bigger requests. There is `ReactiveRequest.Content.fromPublisher` class in Jetty
     *  client, but it seems to cause infinite blocks in tests.
     *  Plus, it has other flows: adding mandatory `Content-Type` header
     *  and `Transfer-Encoding: chunked`. So own implementation might be needed.
     */
    private CompletionStage<Request> request(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final RequestLineFrom req = new RequestLineFrom(line);
        final String scheme;
        if (this.secure) {
            scheme = "https";
        } else {
            scheme = "http";
        }
        final URI uri = req.uri();
        final Request request = this.client.newRequest(
            new URIBuilder()
                .setScheme(scheme)
                .setHost(this.host)
                .setPort(this.port)
                .setPath(uri.getPath())
                .setCustomQuery(uri.getQuery())
                .toString()
        ).method(req.method().value());
        for (final Map.Entry<String, String> header : headers) {
            request.header(header.getKey(), header.getValue());
        }
        return new PublisherAs(body).bytes().thenApply(
            bytes -> {
                final Request result;
                if (bytes.length > 0) {
                    result = request.content(new BytesContentProvider(bytes));
                } else {
                    result = request;
                }
                return result;
            }
        );
    }

    /**
     * Headers from {@link ReactiveResponse}.
     *
     * @since 0.1
     */
    private static class ResponseHeaders extends Headers.Wrap {

        /**
         * Ctor.
         *
         * @param response Response to extract headers from.
         */
        ResponseHeaders(final ReactiveResponse response) {
            super(
                new Headers.From(
                    response.getHeaders().stream()
                        .map(header -> new Header(header.getName(), header.getValue()))
                        .collect(Collectors.toList())
                )
            );
        }
    }
}
