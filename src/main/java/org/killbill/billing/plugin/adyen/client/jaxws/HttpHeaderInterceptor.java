/*
 * Copyright 2014 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.plugin.adyen.client.jaxws;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

public class HttpHeaderInterceptor extends AbstractPhaseInterceptor<Message> {

    @VisibleForTesting
    static final String X_REQUEST_ID = "x-request-id";

    public HttpHeaderInterceptor() {
        super(Phase.POST_PROTOCOL);
    }

    @Override
    public void handleMessage(final Message message) throws Fault {
        final Map<String, List> headers = (Map<String, List>) message.get(Message.PROTOCOL_HEADERS);
        try {
            headers.put(X_REQUEST_ID, ImmutableList.<String>of(UUID.randomUUID().toString()));
        } catch (final Exception e) {
            throw new Fault(e);
        }
    }
}
