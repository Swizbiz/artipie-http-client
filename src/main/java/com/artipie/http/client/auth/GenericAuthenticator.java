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
package com.artipie.http.client.auth;

import com.artipie.http.Headers;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.headers.WwwAuthenticate;
import java.util.concurrent.CompletionStage;
import java.util.stream.StreamSupport;

/**
 * Generic authenticator that performs authentication using username and password.
 * Authentication is done if requested by server using required scheme.
 *
 * @since 0.3
 */
public final class GenericAuthenticator implements Authenticator {

    /**
     * Basic authenticator used when required.
     */
    private final Authenticator basic;

    /**
     * Bearer authenticator used when required.
     */
    private final Authenticator bearer;

    /**
     * Ctor.
     *
     * @param client Client slices.
     */
    public GenericAuthenticator(final ClientSlices client) {
        this(
            Authenticator.ANONYMOUS,
            new BearerAuthenticator(client, new OAuthTokenFormat(), Authenticator.ANONYMOUS)
        );
    }

    /**
     * Ctor.
     *
     * @param client Client slices.
     * @param username Username.
     * @param password Password.
     */
    public GenericAuthenticator(
        final ClientSlices client,
        final String username,
        final String password
    ) {
        this(
            new BasicAuthenticator(username, password),
            new BearerAuthenticator(
                client,
                new OAuthTokenFormat(),
                new BasicAuthenticator(username, password)
            )
        );
    }

    /**
     * Ctor.
     *
     * @param basic Basic authenticator used when required.
     * @param bearer Bearer authenticator used when required.
     */
    public GenericAuthenticator(final Authenticator basic, final Authenticator bearer) {
        this.basic = basic;
        this.bearer = bearer;
    }

    @Override
    public CompletionStage<Headers> authenticate(final Headers headers) {
        return StreamSupport.stream(headers.spliterator(), false)
            .filter(header -> header.getKey().equals(WwwAuthenticate.NAME))
            .findAny()
            .map(header -> this.authenticate(new WwwAuthenticate(header.getValue())))
            .orElse(Authenticator.ANONYMOUS)
            .authenticate(headers);
    }

    /**
     * Get authorization headers.
     *
     * @param header WWW-Authenticate to use for authorization.
     * @return Authorization headers.
     */
    public Authenticator authenticate(final WwwAuthenticate header) {
        final Authenticator result;
        final String scheme = header.scheme();
        if ("Basic".equals(scheme)) {
            result = this.basic;
        } else if ("Bearer".equals(scheme)) {
            result = this.bearer;
        } else {
            throw new IllegalArgumentException(String.format("Unsupported scheme: %s", scheme));
        }
        return result;
    }
}
