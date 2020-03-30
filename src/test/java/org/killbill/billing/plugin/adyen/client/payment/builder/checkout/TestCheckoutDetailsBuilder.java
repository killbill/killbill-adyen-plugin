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

package org.killbill.billing.plugin.adyen.client.payment.builder.checkout;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.api.mapping.TestKlarnaPaymentInfoBase;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.api.PluginProperties;
import org.testng.annotations.Test;

import com.adyen.model.Name;
import com.adyen.model.checkout.LineItem;
import com.adyen.model.checkout.PaymentsDetailsRequest;
import com.adyen.model.checkout.PaymentsRequest;
import com.google.common.collect.ImmutableMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestCheckoutDetailsBuilder extends TestKlarnaPaymentInfoBase {
    private final String countryCode = "GB";
    private final String merchantAccount = "MerchantAccount";
    private final String paymentDataResponse = "AbcdefghijklmnopqrstuvwxyZ1234567890";
    private String authKeyResponse = "{\"key1\":\"text\",\"key2\":\"blob\"}";
    private Iterable<PluginProperty> authCompleteProperties = PluginProperties.buildPluginProperties(
            ImmutableMap.<String,String>builder()
                    .put("key1", "ResponseData1")
                    .put("key2", "ResponseData2")
                    .build());

    @Test(groups = "fast")
    public void testBuildDetailsRequest() throws Exception {
        KlarnaPaymentInfo paymentInfo = getPaymentInfo(merchantAccount,
                                                       countryCode,
                                                       "dummy",
                                                       "dummy",
                                                       "dummy",
                                                       authCompleteProperties);

        //update the saved auth response data
        Map<String, String> responseData = ImmutableMap.<String,String>builder()
                .put("paymentData", paymentDataResponse)
                .put("resultKeys", authKeyResponse)
                .build();
        paymentInfo.setAuthResponseData(responseData);

        PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.valueOf(12), Currency.EUR,
                UUID.randomUUID().toString(), paymentInfo);

        final CheckoutDetailsBuilder builder = new CheckoutDetailsBuilder(
                merchantAccount, paymentData, getUserData());

        PaymentsDetailsRequest request = builder.build();
        Map<String, String> authDetails = request.getDetails();
        assertEquals(request.getPaymentData(), paymentDataResponse);
        assertEquals(authDetails.get("key1"), "ResponseData1");
        assertEquals(authDetails.get("key2"), "ResponseData2");
    }
}
