/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.jaxws;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLoggingOutInterceptor {

    @Test(groups = "fast")
    public void testPayUExpiryYearObfuscation() {
        final String year = "2013";

        final String result = new LoggingOutInterceptor().transform("          <cvc>111</cvc>\n" +
                                                                    "          <ns:expiryMonth>04</expiryMonth>\n" +
                                                                    "          <ns:expiryYear>" + year + "</expiryYear>\n" +
                                                                    "          <ns:holderName>test</holderName>\n" +
                                                                    "          <ns:number>5123456789012346</number>\n");
        Assert.assertTrue(!result.contains(year));
    }

    @Test
    public void testAuthenticationHeaderObfuscation() {
        final String authScheme = "Basic d3NGFueKYWRlx3NDApMjM=";
        final String result = new LoggingOutInterceptor().transform(" Headers: {Accept=[*/*], Authorization=["+ authScheme + "] ");

        Assert.assertFalse(result.contains(authScheme));
    }

    @Test
    public void testPayUExpiryMonthObfuscation() {
        final String month = "04";

        final String result = new LoggingOutInterceptor().transform("          <cvc>111</cvc>\n" +
                                                                    "          <ns:expiryMonth>" + month + "</expiryMonth>\n" +
                                                                    "          <ns:expiryYear>2013</expiryYear>\n" +
                                                                    "          <ns:holderName>test</holderName>\n" +
                                                                    "          <ns:number>5123456789012346</number>\n");
        Assert.assertTrue(!result.contains(month));
    }
}
