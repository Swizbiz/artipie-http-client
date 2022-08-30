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

import com.artipie.asto.test.TestResource;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Tests for {@link JettyClientSlice} with HTTPS server.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class JettyClientSliceSecureTest extends JettyClientSliceTest {

    @Override
    HttpClient newHttpClient() {
        final SslContextFactory factory = new SslContextFactory.Client();
        factory.setTrustAll(true);
        return new HttpClient(factory);
    }

    @Override
    HttpServerOptions newHttpServerOptions() {
        return super.newHttpServerOptions()
            .setSsl(true)
            .setKeyStoreOptions(
                new JksOptions()
                    .setPath(
                        new TestResource("keystore").asPath().toString()
                    )
                    .setPassword("123456")
            );
    }
}
