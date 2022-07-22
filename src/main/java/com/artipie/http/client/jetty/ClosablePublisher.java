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

import hu.akarnokd.rxjava2.interop.MaybeInterop;
import io.reactivex.Flowable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Publisher that subscribes and consumes origin publisher if it was not done yet.
 *
 * @since 0.1
 */
final class ClosablePublisher implements Publisher<ContentChunk> {

    /**
     * Origin publisher.
     */
    private final Publisher<ContentChunk> origin;

    /**
     * Subscribed flag.
     */
    private volatile boolean subscribed;

    /**
     * Ctor.
     *
     * @param origin Origin publisher.
     */
    ClosablePublisher(final Publisher<ContentChunk> origin) {
        this.origin = origin;
    }

    @Override
    public void subscribe(final Subscriber<? super ContentChunk> subscriber) {
        this.subscribed = true;
        this.origin.subscribe(subscriber);
    }

    /**
     * Closes publisher.
     *
     * @return Completion of publisher closing.
     */
    public CompletionStage<Void> close() {
        final CompletionStage<Void> result;
        if (this.subscribed) {
            result = CompletableFuture.allOf();
        } else {
            result = Flowable.fromPublisher(this.origin)
                .lastElement()
                .to(MaybeInterop.get())
                .thenCompose(ignored -> CompletableFuture.allOf());
        }
        return result;
    }
}
