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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestHttpHeaderInterceptor {

    @Test(groups = "fast")
    public void testAddRequestId() throws Exception {
        final HttpHeaderInterceptor interceptor = new HttpHeaderInterceptor();
        Assert.assertEquals(interceptor.getPhase(), Phase.PRE_STREAM);

        final Map<String, List<String>> headers = new HashMap<String, List<String>>();
        final Message message = Mockito.mock(Message.class);
        Mockito.when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);

        Assert.assertNull(headers.get(HttpHeaderInterceptor.X_REQUEST_ID));
        interceptor.handleMessage(message);
        Assert.assertNotNull(headers.get(HttpHeaderInterceptor.X_REQUEST_ID));
    }
}
