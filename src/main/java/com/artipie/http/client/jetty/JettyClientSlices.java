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

import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.Settings;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * ClientSlices implementation using Jetty HTTP client as back-end.
 * <code>start()</code> method should be called before sending responses to initialize
 * underlying client. <code>stop()</code> methods should be used to release resources
 * and stop requests in progress.
 *
 * @since 0.1
 */
public final class JettyClientSlices implements ClientSlices {

    /**
     * Default HTTP port.
     */
    private static final int HTTP_PORT = 80;

    /**
     * Default HTTPS port.
     */
    private static final int HTTPS_PORT = 443;

    /**
     * HTTP client.
     */
    private final HttpClient clnt;

    /**
     * Ctor.
     */
    public JettyClientSlices() {
        this(new Settings.Default());
    }

    /**
     * Ctor.
     *
     * @param settings Settings.
     */
    public JettyClientSlices(final Settings settings) {
        this.clnt = create(settings);
    }

    /**
     * Prepare for usage.
     *
     * @throws Exception In case of any errors starting.
     */
    public void start() throws Exception {
        this.clnt.start();
    }

    /**
     * Release used resources and stop requests in progress.
     *
     * @throws Exception In case of any errors stopping.
     */
    public void stop() throws Exception {
        this.clnt.stop();
    }

    @Override
    public Slice http(final String host) {
        return this.slice(false, host, JettyClientSlices.HTTP_PORT);
    }

    @Override
    public Slice http(final String host, final int port) {
        return this.slice(false, host, port);
    }

    @Override
    public Slice https(final String host) {
        return this.slice(true, host, JettyClientSlices.HTTPS_PORT);
    }

    @Override
    public Slice https(final String host, final int port) {
        return this.slice(true, host, port);
    }

    /**
     * Create slice backed by client.
     *
     * @param secure Secure connection flag.
     * @param host Host name.
     * @param port Port.
     * @return Client slice.
     */
    private Slice slice(final boolean secure, final String host, final int port) {
        return new JettyClientSlice(this.clnt, secure, host, port);
    }

    /**
     * Creates {@link HttpClient} from {@link Settings}.
     *
     * @param settings Settings.
     * @return HTTP client built from settings.
     */
    private static HttpClient create(final Settings settings) {
        final HttpClient result = new HttpClient(new SslContextFactory.Client(settings.trustAll()));
        settings.proxy().ifPresent(
            proxy -> result.getProxyConfiguration().getProxies().add(
                new HttpProxy(new Origin.Address(proxy.host(), proxy.port()), proxy.secure())
            )
        );
        result.setFollowRedirects(settings.followRedirects());
        if (settings.connectTimeout() <= 0) {
            /* @checkstyle MethodBodyCommentsCheck (1 line)
             * Jetty client does not treat zero value as infinite timeout in non-blocking mode.
             * Instead it timeouts the connection instantly.
             * That has been tested in version org.eclipse.jetty:jetty-client:9.4.30.v20200611.
             * See "org.eclipse.jetty.io.ManagedSelector.Connect" class constructor
             * and "run()" method for details.
             * Issue was reported, see https://github.com/eclipse/jetty.project/issues/5150
             */
            result.setConnectBlocking(true);
        }
        result.setConnectTimeout(settings.connectTimeout());
        result.setIdleTimeout(settings.idleTimeout());
        return result;
    }
}
