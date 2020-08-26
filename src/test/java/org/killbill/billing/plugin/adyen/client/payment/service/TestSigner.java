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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class TestSigner {

    @Test(groups = "fast")
    public void testSHA1() {
        final Signer signer = new Signer();
        @SuppressWarnings("deprecation") final String signature = signer.signFormParameters(199L,
                                                                                            "EUR",
                                                                                            "2015-07-01",
                                                                                            "SKINTEST-1435226439255",
                                                                                            "X7hsNDWp",
                                                                                            "TestMerchant",
                                                                                            null,
                                                                                            null,
                                                                                            null,
                                                                                            null,
                                                                                            "2015-06-25T10:31:06Z",
                                                                                            "testing",
                                                                                            "HmacSHA1");
        Assert.assertEquals(signature, "gluOhhUTvBBQjjr336VCE+qNl1o=");
    }

    @Test(groups = "fast")
    public void testSHA256() {
        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("merchantAccount", "TestMerchant");
        builder.put("currencyCode", "EUR");
        builder.put("paymentAmount", "199");
        builder.put("sessionValidity", "2015-06-25T10:31:06Z");
        builder.put("shipBeforeDate", "2015-07-01");
        builder.put("shopperLocale", "en_GB");
        builder.put("merchantReference", "SKINTEST-1435226439255");
        builder.put("skinCode", "X7hsNDWp");
        final Map<String, String> params = builder.build();

        final Signer signer = new Signer();
        final String signature = signer.signFormParameters(params, "4468D9782DEF54FCD706C9100C71EC43932B1EBC2ACF6BA0560C05AAA7550C48", "HmacSHA256");
        Assert.assertEquals(signature, "GJ1asjR5VmkvihDJxCd8yE2DGYOKwWwJCBiV3R51NFg=");
    }

    @Test(groups = "fast")
    public void testSigningString() throws Exception {
        final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        builder.put("countryCode", "NL");
        builder.put("currencyCode", "EUR");
        builder.put("merchantAccount", "NickAnderson");
        builder.put("resURL", "http://localhost:8000");
        builder.put("merchantReference", "TEST-PAYMENT-2016-07-14-17:13:32");
        builder.put("allowedMethods", "mc,visa,amex");
        builder.put("paymentAmount", "199");
        builder.put("shopper.lastName", "Doe");
        builder.put("sessionValidity", "2016-07-15T17:13:32+00:00");
        builder.put("shipBeforeDate", "2016-07-17");
        builder.put("shopper.firstName", "John");
        builder.put("skinCode", "43ZAmyBx");
        builder.put("merchantReturnData", "shopids");
        builder.put("shopperEmail", "test@adyen.com");
        builder.put("shopperLocale", "en_US");
        final Map<String, String> params = builder.build();

        final Signer signer = new Signer();
        Assert.assertEquals(signer.getSigningString(params), "allowedMethods:countryCode:currencyCode:merchantAccount:merchantReference:merchantReturnData:paymentAmount:resURL:sessionValidity:shipBeforeDate:shopper.firstName:shopper.lastName:shopperEmail:shopperLocale:skinCode:mc,visa,amex:NL:EUR:NickAnderson:TEST-PAYMENT-2016-07-14-17\\:13\\:32:shopids:199:http\\://localhost\\:8000:2016-07-15T17\\:13\\:32+00\\:00:2016-07-17:John:Doe:test@adyen.com:en_US:43ZAmyBx");
    }
}
