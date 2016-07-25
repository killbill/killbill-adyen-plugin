/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
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

import java.util.HashMap;

import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.HppCompletedResult;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class TestAdyenPaymentServiceProviderHostedPaymentPagePort {

    private AdyenPaymentServiceProviderHostedPaymentPagePort adyenPaymentServiceProviderHostedPaymentPagePort;

    @BeforeMethod(groups = "fast")
    public void setUp() throws Exception {
        final AdyenConfigProperties adyenConfigProperties = Mockito.mock(AdyenConfigProperties.class);
        final AdyenRequestFactory adyenRequestFactory = Mockito.mock(AdyenRequestFactory.class);
        adyenPaymentServiceProviderHostedPaymentPagePort = new AdyenPaymentServiceProviderHostedPaymentPagePort(adyenConfigProperties, adyenRequestFactory, null);
    }

    @Test(groups = "fast")
    public void parsePspResponseShouldSetNullIfExternalRefIsNotGivenNull() {
        final HppCompletedResult hppCompletedResult = adyenPaymentServiceProviderHostedPaymentPagePort.parseAndVerifyRequestIntegrity(new HashMap<String, String>());
        Assert.assertNull(hppCompletedResult.getPspReference());
    }

    @Test(groups = "fast")
    public void parsePspResponseShouldSetNullIfExternalRefIsNotGivenEmptyString() {
        final HppCompletedResult hppCompletedResult = adyenPaymentServiceProviderHostedPaymentPagePort.parseAndVerifyRequestIntegrity(ImmutableMap.<String, String>of("pspReference", ""));
        Assert.assertNull(hppCompletedResult.getPspReference());
    }

    @Test(groups = "fast")
    public void parsePspResponseShouldSetNullIfExternalRefIsNotGivenEscapedEmptyString() {
        final HppCompletedResult hppCompletedResult = adyenPaymentServiceProviderHostedPaymentPagePort.parseAndVerifyRequestIntegrity(ImmutableMap.<String, String>of("pspReference", "\"\""));
        Assert.assertNull(hppCompletedResult.getPspReference());
    }

    @Test(groups = "fast")
    public void parsePspResponseShouldSetTheExternalRefIfItIsGiven() {
        final String expectedPspReference = "123456";

        final HppCompletedResult hppCompletedResult = adyenPaymentServiceProviderHostedPaymentPagePort.parseAndVerifyRequestIntegrity(ImmutableMap.<String, String>of("pspReference", expectedPspReference));
        Assert.assertEquals(hppCompletedResult.getPspReference(), expectedPspReference);
    }
}
