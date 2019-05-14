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

package org.gradle.workers.internal;

import org.gradle.initialization.GradleApiUtil;
import org.gradle.internal.classloader.FilteringClassLoader;
import org.gradle.internal.classloader.VisitableURLClassLoader;
import org.gradle.internal.classpath.DefaultClassPath;
import org.gradle.internal.reflect.DirectInstantiator;

import java.io.File;

public class IsolatedClassLoaderUtil {
    private static ClassLoaderStructure getDefaultClassLoaderStructure(ClassLoader workerInfrastructureClassloader, Iterable<File> userClasspath) {
        FilteringClassLoader.Spec gradleApiFilter = GradleApiUtil.apiSpecFor(workerInfrastructureClassloader, DirectInstantiator.INSTANCE);
        VisitableURLClassLoader.Spec userSpec = new VisitableURLClassLoader.Spec("worker-loader", DefaultClassPath.of(userClasspath).getAsURLs());

        // Add the Gradle API filter between the user classloader and the worker infrastructure classloader
        return new ClassLoaderStructure(gradleApiFilter).withChild(userSpec);
    }

    public static ClassLoaderStructure getDefaultClassLoaderStructure(Iterable<File> userClasspath) {
        return getDefaultClassLoaderStructure(IsolatedClassLoaderUtil.class.getClassLoader(), userClasspath);
    }
}
