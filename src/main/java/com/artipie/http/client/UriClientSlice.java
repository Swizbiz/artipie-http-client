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
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Client slice that sends requests to host and port using scheme specified in URI.
 * If URI contains path then it is used as prefix. Other URI components are ignored.
 *
 * @since 0.3
 */
public final class UriClientSlice implements Slice {

    /**
     * Client slices.
     */
    private final ClientSlices client;

    /**
     * URI.
     */
    private final URI uri;

    /**
     * Ctor.
     *
     * @param client Client slices.
     * @param uri URI.
     */
    public UriClientSlice(final ClientSlices client, final URI uri) {
        this.client = client;
        this.uri = uri;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Slice slice;
        final String path = this.uri.getRawPath();
        if (path == null) {
            slice = this.base();
        } else {
            slice = new PathPrefixSlice(this.base(), path);
        }
        return slice.response(line, headers, body);
    }

    /**
     * Get base client slice by scheme, host and port of URI ignoring path.
     *
     * @return Client slice.
     */
    private Slice base() {
        final Slice slice;
        final String scheme = this.uri.getScheme();
        final String host = this.uri.getHost();
        final int port = this.uri.getPort();
        switch (scheme) {
            case "https":
                if (port > 0) {
                    slice = this.client.https(host, port);
                } else {
                    slice = this.client.https(host);
                }
                break;
            case "http":
                if (port > 0) {
                    slice = this.client.http(host, port);
                } else {
                    slice = this.client.http(host);
                }
                break;
            default:
                throw new IllegalStateException(
                    String.format("Scheme '%s' is not supported", scheme)
                );
        }
        return slice;
    }
}
