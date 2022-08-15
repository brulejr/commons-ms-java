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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import io.jrb.labs.commons.model.Entity;
import io.jrb.labs.commons.model.User;
import io.jrb.labs.commons.repository.EntityRepository;
import io.jrb.labs.commons.resource.Resource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

public class R2dbcCrudServiceUtils<E extends Entity<E>, R extends Resource<R>> implements CrudServiceUtils<E, R> {

    private final String entityType;
    private final EntityRepository<E> entityRepository;
    private final ObjectMapper objectMapper;

    public R2dbcCrudServiceUtils(
            final String entityType,
            final EntityRepository<E> entityRepository,
            final ObjectMapper objectMapper
    ) {
        this.entityType = entityType;
        this.entityRepository = entityRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public R applyPatch(final UUID guid, final JsonPatch patch, final R resource, final Class<R> resourceClass) {
        try {
            final JsonNode patched = patch.apply(objectMapper.convertValue(resource, JsonNode.class));
            return objectMapper.treeToValue(patched, resourceClass);
        } catch (final Exception e) {
            throw new PatchInvalidException(resourceClass.getSimpleName(), guid, e);
        }
    }

    @Override
    public Mono<R> createEntity(final User user, final E entity, final Function<E, Mono<R>> fnCreate) {
        return Mono.just(entity.withGuid(UUID.randomUUID()))
                .flatMap(entityRepository::save)
                .flatMap(fnCreate)
                .onErrorResume(handleMonoError(t -> new CreateEntityException(entityType, t)));
    }

    @Override
    public Mono<E> deleteEntity(User user, UUID guid, Function<E, Mono<E>> fnDelete) {
        return entityRepository.findByUserIdAndGuid(user.getId(), guid)
                .flatMap(entity ->
                        Mono.just(entity)
                                .switchIfEmpty(Mono.error(new EntityNotFoundException(entityType, guid)))
                                .flatMap(fnDelete)
                                .flatMap(entityRepository::delete)
                                .thenReturn(entity)
                                .onErrorResume(handleMonoError(t -> new DeleteEntityException(entityType, guid, t)))
                );
    }

    @Override
    public Mono<R> findEntity(User user, UUID guid, Function<E, Mono<R>> fnFind) {
        return entityRepository.findByUserIdAndGuid(user.getId(), guid)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(entityType, guid)))
                .flatMap(fnFind)
                .onErrorResume(handleMonoError(t -> new FindEntityException(entityType, guid, t)));
    }

    @Override
    public Flux<R> retrieveEntities(User user, Function<E, Mono<R>> fnRetrieve) {
        return entityRepository.findAllByUserId(user.getId())
                .flatMap(fnRetrieve)
                .onErrorResume(handleFluxError(t -> new RetrieveEntitiesException(entityType, t)));
    }

    @Override
    public Mono<R> updateEntity(User user, UUID guid, Function<E, E> fnEntity, Function<E, Mono<R>> fnUpdate) {
        return entityRepository.findByUserIdAndGuid(user.getId(), guid)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(entityType, guid)))
                .map(fnEntity)
                .flatMap(entityRepository::save)
                .flatMap(fnUpdate)
                .onErrorResume(handleMonoError(t -> new UpdateEntityException(entityType, guid, t)));
    }

}
