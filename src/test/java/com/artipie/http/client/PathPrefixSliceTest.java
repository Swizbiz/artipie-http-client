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
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Headers;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link PathPrefixSlice}.
 *
 * @since 0.3
 * @checkstyle ParameterNumberCheck (500 lines)
 */
final class PathPrefixSliceTest {

    @ParameterizedTest
    @CsvSource({
        "'',/,/,",
        "/prefix,/,/prefix/,",
        "/a/b/c,/d/e/f,/a/b/c/d/e/f,",
        "/my/repo,/123/file.txt?param1=foo&param2=bar,/my/repo/123/file.txt,param1=foo&param2=bar",
        "/aaa/bbb,/%26/file.txt?p=%20%20,/aaa/bbb/%26/file.txt,p=%20%20"
    })
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    void shouldAddPrefixToPathAndPreserveEverythingElse(
        final String prefix, final String line, final String path, final String query
    ) {
        final RqMethod method = RqMethod.GET;
        final Headers headers = new Headers.From("X-Header", "The Value");
        final byte[] body = "request body".getBytes();
        new PathPrefixSlice(
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
                MatcherAssert.assertThat(
                    "Method is preserved",
                    new RequestLineFrom(rsline).method(),
                    new IsEqual<>(method)
                );
                MatcherAssert.assertThat(
                    "Headers are preserved",
                    rqheaders,
                    new IsEqual<>(headers)
                );
                MatcherAssert.assertThat(
                    "Body is preserved",
                    new PublisherAs(rqbody).bytes().toCompletableFuture().join(),
                    new IsEqual<>(body)
                );
                return StandardRs.OK;
            },
            prefix
        ).response(
            new RequestLine(method, line).toString(),
            headers,
            new Content.From(body)
        ).send(
            (status, rsheaders, rsbody) -> CompletableFuture.allOf()
        );
    }
}
