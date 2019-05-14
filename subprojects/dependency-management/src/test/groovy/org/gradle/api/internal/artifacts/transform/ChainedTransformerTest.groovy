/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.internal.artifacts.transform

import com.google.common.collect.ImmutableList
import org.gradle.api.Action
import org.gradle.execution.ProjectExecutionServiceRegistry
import org.gradle.internal.Try
import spock.lang.Specification

import javax.annotation.Nullable

class ChainedTransformerTest extends Specification {
    private TransformationSubject initialSubject = TransformationSubject.initial(new File("foo"))

    def "applies second transform on the result of the first"() {
        given:
        def chain = new TransformationChain(new CachingTransformation(), new NonCachingTransformation())

        expect:
        chain.transform(initialSubject, Mock(ExecutionGraphDependenciesResolver), null).get().files == [new File("foo/cached/non-cached")]
    }

    class CachingTransformation extends AbstractTestTransformation {

        @Override
        Try<TransformationSubject> transform(TransformationSubject subjectToTransform, ExecutionGraphDependenciesResolver dependenciesResolver, ProjectExecutionServiceRegistry services) {
            return Try.successful(subjectToTransform.createSubjectFromResult(ImmutableList.of(new File(subjectToTransform.files.first(), "cached"))))
        }

        @Override
        String getDisplayName() {
            return "cached"
        }

    }

    class NonCachingTransformation extends AbstractTestTransformation {

        @Override
        Try<TransformationSubject> transform(TransformationSubject subjectToTransform, ExecutionGraphDependenciesResolver dependenciesResolver, ProjectExecutionServiceRegistry services) {
            return Try.successful(subjectToTransform.createSubjectFromResult(ImmutableList.of(new File(subjectToTransform.files.first(), "non-cached"))))
        }

        @Override
        String getDisplayName() {
            return "non-cached"
        }
    }

    abstract class AbstractTestTransformation implements Transformation {
        @Override
        Transformation.TransformationContinuation<TransformationSubject> prepareTransform(TransformationSubject subjectToTransform, ExecutionGraphDependenciesResolver dependenciesResolver, @Nullable ProjectExecutionServiceRegistry services) {
            return new Transformation.TransformationContinuation<TransformationSubject>() {
                @Override
                boolean isExpensive() {
                    return true
                }

                @Override
                Try<TransformationSubject> invoke() {
                    return transform(subjectToTransform, dependenciesResolver, services)
                }
            }
        }

        @Override
        boolean requiresDependencies() {
            return false
        }

        @Override
        void visitTransformationSteps(Action<? super TransformationStep> action) {
        }


        @Override
        boolean endsWith(Transformation otherTransform) {
            return false
        }

        @Override
        int stepsCount() {
            return 1
        }
    }
}
