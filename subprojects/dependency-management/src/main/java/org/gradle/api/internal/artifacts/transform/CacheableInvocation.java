/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.transform;

import org.gradle.internal.Try;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface CacheableInvocation<T> {
    Optional<Try<T>> getCachedResult();

    Try<T> invoke();

    default <U> CacheableInvocation<U> map(Function<? super T, U> mapper) {
        return new CacheableInvocation<U>() {
            @Override
            public Optional<Try<U>> getCachedResult() {
                return CacheableInvocation.this.getCachedResult().map(result -> result.map(mapper));
            }

            @Override
            public Try<U> invoke() {
                return CacheableInvocation.this.invoke().map(mapper);
            }
        };
    }

    default <U> CacheableInvocation<U> flatMap(Function<? super T, CacheableInvocation<U>> mapper) {
        Optional<CacheableInvocation<U>> cachedInvocation = getCachedResult()
            .map(cachedResult -> cachedResult.map(mapper).getSuccessfulOrElse(
                Function.identity(),
                failure -> cached(Try.failure(failure))
            ));
        return cachedInvocation.orElseGet(() ->
            nonCached(() ->
                invoke().flatMap(intermediateResult -> mapper.apply(intermediateResult).invoke())
            )
        );
    }

    static <T> CacheableInvocation<T> cached(Try<T> result) {
        return new CacheableInvocation<T>() {
            @Override
            public Optional<Try<T>> getCachedResult() {
                return Optional.of(result);
            }

            @Override
            public Try<T> invoke() {
                return result;
            }
        };
    }

    static <T> CacheableInvocation<T> nonCached(Supplier<Try<T>> result) {
        return new CacheableInvocation<T>() {
            @Override
            public Optional<Try<T>> getCachedResult() {
                return Optional.empty();
            }

            @Override
            public Try<T> invoke() {
                return result.get();
            }
        };
    }
}
