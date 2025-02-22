/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.internal.http;

import com.arangodb.ArangoDBException;
import com.arangodb.internal.net.*;
import com.arangodb.internal.util.HostUtils;
import com.arangodb.internal.util.RequestUtils;
import com.arangodb.util.ArangoSerialization;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author Mark Vollmary
 */
public class HttpCommunication implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCommunication.class);

    public static class Builder {

        private final HostHandler hostHandler;
        private final Map<String, String> headerParam;

        public Builder(final HostHandler hostHandler, Map<String, String> headerParam) {
            super();
            this.hostHandler = hostHandler;
            this.headerParam = headerParam;
        }

        public Builder(final Builder builder) {
            this(builder.hostHandler, builder.headerParam);
        }

        public HttpCommunication build(final ArangoSerialization util) {
            return new HttpCommunication(hostHandler, headerParam);
        }
    }

    private final HostHandler hostHandler;

    private final Map<String, String> headerParam;

    private HttpCommunication(final HostHandler hostHandler, Map<String, String> headerParam) {
        super();
        this.hostHandler = hostHandler;
        this.headerParam = headerParam;
    }

    @Override
    public void close() throws IOException {
        hostHandler.close();
    }

    public Response execute(final Request request, final HostHandle hostHandle) throws ArangoDBException {
        return execute(request, hostHandle, 0);
    }

    private Response execute(final Request request, final HostHandle hostHandle, final int attemptCount) throws ArangoDBException {
        final AccessType accessType = RequestUtils.determineAccessType(request);
        Host host = hostHandler.get(hostHandle, accessType);
        try {
            while (true) {
                try {
                    final HttpConnection connection = (HttpConnection) host.connection();
                    for (Map.Entry<String, String> entry : this.headerParam.entrySet()) {
                        request.putHeaderParam(entry.getKey(), entry.getValue());
                    }
                    final Response response = connection.execute(request);
                    hostHandler.success();
                    hostHandler.confirm();
                    return response;
                } catch (final SocketTimeoutException e) {
                    // SocketTimeoutException exceptions are wrapped and rethrown.
                    // Differently from other IOException exceptions they must not be retried,
                    // since the requests could not be idempotent.
                    TimeoutException te = new TimeoutException(e.getMessage());
                    te.initCause(e);
                    throw new ArangoDBException(te);
                } catch (final IOException e) {
                    hostHandler.fail(e);
                    if (hostHandle != null && hostHandle.getHost() != null) {
                        hostHandle.setHost(null);
                    }
                    final Host failedHost = host;
                    host = hostHandler.get(hostHandle, accessType);
                    if (host != null) {
                        LOGGER.warn(String.format("Could not connect to %s", failedHost.getDescription()), e);
                        LOGGER.warn(String.format("Could not connect to %s. Try connecting to %s",
                                failedHost.getDescription(), host.getDescription()));
                    } else {
                        LOGGER.error(e.getMessage(), e);
                        throw new ArangoDBException(e);
                    }
                }
            }
        } catch (final ArangoDBException e) {
            if (e instanceof ArangoDBRedirectException && attemptCount < 3) {
                final String location = ((ArangoDBRedirectException) e).getLocation();
                final HostDescription redirectHost = HostUtils.createFromLocation(location);
                hostHandler.failIfNotMatch(redirectHost, e);
                return execute(request, new HostHandle().setHost(redirectHost), attemptCount + 1);
            } else {
                throw e;
            }
        }
    }

}
