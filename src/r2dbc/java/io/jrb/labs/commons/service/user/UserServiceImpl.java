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
package io.jrb.labs.commons.service.user;

import io.jrb.labs.commons.model.UserEntity;
import io.jrb.labs.commons.repository.UserEntityRepository;
import io.jrb.labs.commons.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserEntityRepository userEntityRepository;

    public UserServiceImpl(final UserEntityRepository userEntityRepository) {
        this.userEntityRepository = userEntityRepository;
    }

    @Override
    public Mono<UserEntity> loadUser(final UserContext userContext) {
        return userEntityRepository.findByUserguid(userContext.getSubject())
                .switchIfEmpty(newUserTemplate())
                .zipWhen(userEntity -> updateUser(userEntity, userContext))
                .flatMap(tuple -> {
                    final UserEntity oldUser = tuple.getT1();
                    final UserEntity newUser = tuple.getT2();
                   if (newUser.getId() == null || !newUser.equals(oldUser)) {
                       log.info("Saving user {}", newUser);
                       return userEntityRepository.save(newUser);
                   } else {
                       return Mono.just(oldUser);
                   }
                });
    }

    private Mono<UserEntity> newUserTemplate() {
        return Mono.just(UserEntity.builder().build());
    }

    private Mono<UserEntity> updateUser(final UserEntity user, final UserContext userContext) {
        return Mono.just(user.toBuilder()
                .username(userContext.getUsername())
                .userguid(userContext.getSubject())
                .build());
    }

}