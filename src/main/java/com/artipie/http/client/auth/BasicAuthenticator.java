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
import com.artipie.http.headers.Authorization;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Basic authenticator for given username and password.
 *
 * @since 0.3
 */
public final class BasicAuthenticator implements Authenticator {

    /**
     * Username.
     */
    private final String username;

    /**
     * Password.
     */
    private final String password;

    /**
     * Ctor.
     *
     * @param username Username.
     * @param password Password.
     */
    public BasicAuthenticator(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public CompletionStage<Headers> authenticate(final Headers headers) {
        return CompletableFuture.completedFuture(
            new Headers.From(new Authorization.Basic(this.username, this.password))
        );
    }
}
