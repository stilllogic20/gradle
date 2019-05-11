/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.publication.maven.internal.wagon;

import com.google.common.collect.Lists;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransport;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.resource.ExternalResource;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ReadableContent;

import java.io.File;
import java.net.URI;
import java.util.List;

public class RepositoryTransportWagonAdapter {
    private final RepositoryTransport transport;
    private final URI rootUri;
    private final List<PutRequest> putRequests = Lists.newArrayList();

    public RepositoryTransportWagonAdapter(RepositoryTransport transport, URI rootUri) {
        this.transport = transport;
        this.rootUri = rootUri;
    }

    public boolean getRemoteFile(File destination, String resourceName) throws ResourceException {
        ExternalResourceName location = getLocationForResource(resourceName);
        ExternalResource resource = transport.getRepository().resource(location);
        return resource.writeToIfPresent(destination) != null;
    }

    public void putRemoteFile(ReadableContent content, String resourceName) throws ResourceException {
        putRequests.add(new PutRequest(resourceName, content));
    }

    public void publishPending() {
        for (PutRequest putRequest : putRequests) {
            transport.getRepository().withProgressLogging().resource(getLocationForResource(putRequest.resourceName)).put(putRequest.content);
        }
    }

    private ExternalResourceName getLocationForResource(String resource) {
        return new ExternalResourceName(rootUri, resource);
    }

    private class PutRequest {
        final String resourceName;
        final ReadableContent content;

        public PutRequest(String resourceName, ReadableContent content) {
            this.resourceName = resourceName;
            this.content = content;
        }
    }
}
