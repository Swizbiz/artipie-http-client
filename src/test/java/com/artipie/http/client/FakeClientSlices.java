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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fake {@link ClientSlices} implementation that returns specified result
 * and captures last method call.
 *
 * @since 0.3
 */
public final class FakeClientSlices implements ClientSlices {

    /**
     * Captured scheme. True - secure HTTPS protocol, false - insecure HTTP.
     */
    private final AtomicReference<Boolean> csecure;

    /**
     * Captured host.
     */
    private final AtomicReference<String> chost;

    /**
     * Captured port.
     */
    private final AtomicReference<Integer> cport;

    /**
     * Slice returned by requests.
     */
    private final Slice result;

    /**
     * Ctor.
     *
     * @param result Slice returned by requests.
     */
    public FakeClientSlices(final Slice result) {
        this.result = result;
        this.csecure = new AtomicReference<>();
        this.chost = new AtomicReference<>();
        this.cport = new AtomicReference<>();
    }

    /**
     * Get captured scheme.
     *
     * @return Scheme.
     */
    public Boolean capturedSecure() {
        return this.csecure.get();
    }

    /**
     * Get captured host.
     *
     * @return Host.
     */
    public String capturedHost() {
        return this.chost.get();
    }

    /**
     * Get captured port.
     *
     * @return Port.
     */
    public Integer capturedPort() {
        return this.cport.get();
    }

    @Override
    public Slice http(final String host) {
        this.csecure.set(false);
        this.chost.set(host);
        this.cport.set(null);
        return this.result;
    }

    @Override
    public Slice http(final String host, final int port) {
        this.csecure.set(false);
        this.chost.set(host);
        this.cport.set(port);
        return this.result;
    }

    @Override
    public Slice https(final String host) {
        this.csecure.set(true);
        this.chost.set(host);
        this.cport.set(null);
        return this.result;
    }

    @Override
    public Slice https(final String host, final int port) {
        this.csecure.set(true);
        this.chost.set(host);
        this.cport.set(port);
        return this.result;
    }
}
