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

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionEventSupport;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.gradle.api.GradleException;

import java.io.File;
import java.util.List;

import static org.apache.maven.wagon.events.SessionEvent.SESSION_DISCONNECTING;
import static org.apache.maven.wagon.events.SessionEvent.SESSION_LOGGED_IN;
import static org.apache.maven.wagon.events.SessionEvent.SESSION_LOGGED_OFF;
import static org.apache.maven.wagon.events.SessionEvent.SESSION_OPENED;

/**
 * A maven wagon intended to work with {@link org.apache.maven.artifact.manager.DefaultWagonManager} Maven uses reflection to initialize instances of this wagon see: {@link
 * org.codehaus.plexus.component.factory.java.JavaComponentFactory#newInstance(org.codehaus.plexus.component.repository.ComponentDescriptor, org.codehaus.classworlds.ClassRealm,
 * org.codehaus.plexus.PlexusContainer)}
 */
public class RepositoryTransportDeployWagon implements Wagon {

    private static final ThreadLocal<RepositoryTransportWagonAdapter> CURRENT_DELEGATE = new InheritableThreadLocal<RepositoryTransportWagonAdapter>();

    private SessionEventSupport sessionEventSupport = new SessionEventSupport();
    private Repository mutatingRepository;

    public static void contextualize(RepositoryTransportWagonAdapter adapter) {
        CURRENT_DELEGATE.set(adapter);
    }

    public static void decontextualize() {
        CURRENT_DELEGATE.remove();
    }

    @Override
    public final void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        try {
            if (!destination.exists()) {
                destination.getParentFile().mkdirs();
                destination.createNewFile();
            }
            if (!getDelegate().getRemoteFile(destination, resourceName)) {
                throw new ResourceDoesNotExistException(String.format("Resource '%s' does not exist", resourceName));
            }
        } catch (ResourceDoesNotExistException e) {
            throw e;
        } catch (Exception e) {
            throw new TransferFailedException(String.format("Could not get resource '%s'", resourceName), e);
        }
    }

    @Override
    public final void put(File file, String resourceName) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        try {
            getDelegate().putRemoteFile(file, resourceName);
        } catch (Exception e) {
            throw new TransferFailedException(String.format("Could not write to resource '%s'", resourceName), e);
        }
    }

    private RepositoryTransportWagonAdapter getDelegate() {
        return CURRENT_DELEGATE.get();
    }

    @Override
    public final boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {
        throwNotImplemented("getIfNewer(String resourceName, File file, long timestamp)");
        return false;
    }

    @Override
    public final boolean getIfNewer(String resourceName, File file, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        throwNotImplemented("getIfNewer(String resourceName, File file, long timestamp)");
        return false;
    }

    @Override
    public final void putDirectory(File file, String resourceName) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        throwNotImplemented("putDirectory(File file, String resourceName)");
    }

    @Override
    public final List getFileList(String resourceName) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        throwNotImplemented("getFileList(String resourceName)");
        return null;
    }

    @Override
    public final boolean supportsDirectoryCopy() {
        return false;
    }

    @Override
    public final Repository getRepository() {
        return this.mutatingRepository;
    }

    @Override
    public final void openConnection() throws ConnectionException, AuthenticationException {
    }

    @Override
    public final void connect(Repository repository) throws ConnectionException, AuthenticationException {
        this.mutatingRepository = repository;
        this.sessionEventSupport.fireSessionLoggedIn(sessionEvent(SESSION_LOGGED_IN));
        this.sessionEventSupport.fireSessionOpened(sessionEvent(SESSION_OPENED));
    }

    @Override
    public final void connect(Repository repository, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {
        connect(repository);
    }

    @Override
    public final void connect(Repository repository, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {
        connect(repository);
    }

    @Override
    public final void connect(Repository repository, AuthenticationInfo authenticationInfo) throws ConnectionException, AuthenticationException {
        connect(repository);
    }

    @Override
    public final void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {
        connect(repository);
    }

    @Override
    public final void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {
        connect(repository);
    }

    @Override
    public final void disconnect() throws ConnectionException {
        this.sessionEventSupport.fireSessionDisconnecting(sessionEvent(SESSION_DISCONNECTING));
        this.sessionEventSupport.fireSessionLoggedOff(sessionEvent(SESSION_LOGGED_OFF));
        this.sessionEventSupport.fireSessionDisconnected(sessionEvent(SESSION_LOGGED_OFF));
    }

    @Override
    public final void addSessionListener(SessionListener sessionListener) {
        this.sessionEventSupport.addSessionListener(sessionListener);
    }

    @Override
    public final void removeSessionListener(SessionListener sessionListener) {
        this.sessionEventSupport.removeSessionListener(sessionListener);
    }

    @Override
    public final boolean hasSessionListener(SessionListener sessionListener) {
        return this.sessionEventSupport.hasSessionListener(sessionListener);
    }

    @Override
    public final void addTransferListener(TransferListener transferListener) {

    }

    @Override
    public final void removeTransferListener(TransferListener transferListener) {

    }

    @Override
    public final boolean hasTransferListener(TransferListener transferListener) {
        return false;
    }

    @Override
    public final boolean isInteractive() {
        return false;
    }

    @Override
    public final void setInteractive(boolean b) {

    }

    @Override
    public final void setTimeout(int i) {

    }

    @Override
    public final int getTimeout() {
        return 0;
    }

    @Override
    public final void setReadTimeout(int i) {

    }

    @Override
    public final int getReadTimeout() {
        return 0;
    }

    private SessionEvent sessionEvent(int e) {
        return new SessionEvent(this, e);
    }

    private void throwNotImplemented(String s) {
        throw new GradleException("This wagon does not yet support the method:" + s);
    }
}
