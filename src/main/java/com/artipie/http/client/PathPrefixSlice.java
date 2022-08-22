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

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice that forwards all requests to origin slice prepending path with specified prefix.
 *
 * @since 0.3
 */
public final class PathPrefixSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Prefix.
     */
    private final String prefix;

    /**
     * Ctor.
     *
     * @param origin Origin slice.
     * @param prefix Prefix.
     */
    public PathPrefixSlice(final Slice origin, final String prefix) {
        this.origin = origin;
        this.prefix = prefix;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final RequestLineFrom rqline = new RequestLineFrom(line);
        final URI original = rqline.uri();
        final String uri;
        if (original.getRawQuery() == null) {
            uri = String.format("%s%s", this.prefix, original.getRawPath());
        } else {
            uri = String.format(
                "%s%s?%s",
                this.prefix,
                original.getRawPath(),
                original.getRawQuery()
            );
        }
        return this.origin.response(
            new RequestLine(rqline.method().value(), uri, rqline.version()).toString(),
            headers,
            body
        );
    }
}
