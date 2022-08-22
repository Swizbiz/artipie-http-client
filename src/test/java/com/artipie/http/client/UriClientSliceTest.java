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

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link UriClientSlice}.
 *
 * @since 0.3
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
final class UriClientSliceTest {

    @ParameterizedTest
    @CsvSource({
        "https://artipie.com,true,artipie.com,",
        "http://github.com,false,github.com,",
        "https://github.io:54321,true,github.io,54321",
        "http://localhost:8080,false,localhost,8080"
    })
    void shouldGetClientBySchemeHostPort(
        final String uri, final Boolean secure, final String host, final Integer port
    ) throws Exception {
        final FakeClientSlices fake = new FakeClientSlices((line, headers, body) -> StandardRs.OK);
        new UriClientSlice(
            fake,
            new URI(uri)
        ).response(
            new RequestLine(RqMethod.GET, "/").toString(),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, rsheaders, rsbody) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Scheme is correct",
            fake.capturedSecure(),
            new IsEqual<>(secure)
        );
        MatcherAssert.assertThat(
            "Host is correct",
            fake.capturedHost(),
            new IsEqual<>(host)
        );
        MatcherAssert.assertThat(
            "Port is correct",
            fake.capturedPort(),
            new IsEqual<>(port)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "http://hostname,/,/,",
        "http://hostname/aaa/bbb,/%26/file.txt?p=%20%20,/aaa/bbb/%26/file.txt,p=%20%20"
    })
    void shouldAddPrefixToPathAndPreserveQuery(
        final String uri, final String line, final String path, final String query
    ) throws Exception {
        new UriClientSlice(
            new FakeClientSlices(
                (rsline, rqheaders, rqbody) -> {
                    MatcherAssert.assertThat(
                        "Path is modified",
                        new RequestLineFrom(rsline).uri().getRawPath(),
                        new IsEqual<>(path)
                    );
                    MatcherAssert.assertThat(
                        "Query is preserved",
                        new RequestLineFrom(rsline).uri().getRawQuery(),
                        new IsEqual<>(query)
                    );
                    return StandardRs.OK;
                }
            ),
            new URI(uri)
        ).response(
            new RequestLine(RqMethod.GET, line).toString(),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, rsheaders, rsbody) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
    }
}
