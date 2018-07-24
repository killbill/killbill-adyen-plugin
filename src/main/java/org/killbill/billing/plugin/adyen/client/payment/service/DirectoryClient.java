/*
 * Copyright 2016-2018 Groupon, Inc
 * Copyright 2016-2018 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.killbill.billing.plugin.util.http.HttpClient;
import org.killbill.billing.plugin.util.http.InvalidRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class DirectoryClient extends HttpClient {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryClient.class);

    public DirectoryClient(final String url,
                           final String proxyHost,
                           final Integer proxyPort,
                           final Boolean strictSSL,
                           final int connectTimeoutMs,
                           final int readTimeoutMs) throws GeneralSecurityException {
        super(url, null, null, proxyHost, proxyPort, strictSSL, connectTimeoutMs, readTimeoutMs);
    }

    public Map getDirectory(final Map<String, String> params) {
        try {
            return doCall("POST", "", null, params, Map.class);
        } catch (final InterruptedException e) {
            logger.warn("Unable to retrieve HPP directory for params: {}", params, e);
            return ImmutableMap.<String, String>of();
        } catch (final ExecutionException e) {
            logger.warn("Unable to retrieve HPP directory for params: {}", params, e);
            return ImmutableMap.<String, String>of();
        } catch (final TimeoutException e) {
            logger.warn("Unable to retrieve HPP directory for params: {}", params, e);
            return ImmutableMap.<String, String>of();
        } catch (final IOException e) {
            logger.warn("Unable to retrieve HPP directory for params: {}", params, e);
            return ImmutableMap.<String, String>of();
        } catch (final URISyntaxException e) {
            logger.warn("Unable to retrieve HPP directory for params: {}", params, e);
            return ImmutableMap.<String, String>of();
        } catch (final InvalidRequest invalidRequest) {
            logger.warn("Unable to retrieve HPP directory for params: {}", params, invalidRequest);
            return ImmutableMap.<String, String>of();
        }
    }
}
