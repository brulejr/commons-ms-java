/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.commons.service.crud;

import com.github.fge.jsonpatch.JsonPatch;
import io.jrb.labs.commons.model.Entity;
import io.jrb.labs.commons.model.User;
import io.jrb.labs.commons.resource.Resource;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

public interface CrudServiceUtils<E extends Entity<E>, R extends Resource<R>> {

    R applyPatch(UUID guid, JsonPatch patch, R resource, Class<R> resourceClass);

    Mono<R> createEntity(User user, E entity, Function<E, Mono<R>> fnCreate);

    Mono<E> deleteEntity(User user, UUID guid, Function<E, Mono<E>> fnDelete);

    Mono<R> findEntity(User user, UUID guid, Function<E, Mono<R>> fnFind);

    Flux<R> retrieveEntities(User user, Function<E, Mono<R>> fnRetrieve);

    Mono<R> updateEntity(User user, UUID guid, Function<E, E> fnEntity, Function<E, Mono<R>> fnUpdate);

    default <T> Function<? super Throwable, ? extends Publisher<? extends T>> handleFluxError(
            final Function<? super Throwable, CrudServiceException> errorHandler
    ) {
        return t -> Mono.error((t instanceof CrudServiceException) ? t : errorHandler.apply(t));
    }

    default <T> Function<? super Throwable, ? extends Mono<? extends T>> handleMonoError(
            final Function<? super Throwable, CrudServiceException> errorHandler
    ) {
        return t -> Mono.error((t instanceof CrudServiceException) ? t : errorHandler.apply(t));
    }

}
