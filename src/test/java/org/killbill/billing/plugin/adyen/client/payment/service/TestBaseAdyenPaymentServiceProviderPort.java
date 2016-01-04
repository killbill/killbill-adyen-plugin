/*
 * Copyright 2016 Groupon, Inc
 * Copyright 2016 The Billing Project, LLC
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

import java.math.BigDecimal;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBaseAdyenPaymentServiceProviderPort {

    @Test(groups = "fast", description = "https://github.com/killbill/killbill-adyen-plugin/issues/44")
    public void testMoneyConversion() {
        final BaseAdyenPaymentServiceProviderPort port = new BaseAdyenPaymentServiceProviderPortTester();
        Assert.assertEquals(0L, (long) port.toMinorUnits("USD", BigDecimal.ZERO));
        Assert.assertEquals(100L, (long) port.toMinorUnits("USD", BigDecimal.ONE));
        Assert.assertEquals(1000L, (long) port.toMinorUnits("USD", BigDecimal.TEN));
        Assert.assertEquals(1000L, (long) port.toMinorUnits("EUR", new BigDecimal("10.000000000")));
        // From https://docs.adyen.com/display/TD/HPP+currency+codes: 10 GBP is submitted as 1000, whereas 10 JPY is submitted as 10.
        Assert.assertEquals(1000L, (long) port.toMinorUnits("GBP", new BigDecimal("10.000000000")));
        Assert.assertEquals(10, (long) port.toMinorUnits("JPY", new BigDecimal("10.000000000")));
    }

    private static final class BaseAdyenPaymentServiceProviderPortTester extends BaseAdyenPaymentServiceProviderPort {}
}
