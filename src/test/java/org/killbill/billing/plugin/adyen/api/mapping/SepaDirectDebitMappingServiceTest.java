/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.api.mapping;

import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.SepaDirectDebit;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SepaDirectDebitMappingServiceTest {

    private AccountData accountData;
    private AdyenPaymentMethodsRecord paymentMethodRecord;

    @BeforeMethod(groups = "fast")
    public void initialize() {
        accountData = mock(AccountData.class);
        paymentMethodRecord = mock(AdyenPaymentMethodsRecord.class);
    }

    @Test(groups = "fast")
    public void testSepaMappingServiceWithSepaCountryCode() throws Exception {
        SepaDirectDebit paymentInfo = SepaDirectDebitMappingService.toPaymentInfo(accountData, paymentMethodRecord,
                                                                    ImmutableList.of(new PluginProperty("sepaCountryCode", "UK", false),
                                                                                     new PluginProperty("country", "DE", false)));
        Assert.assertEquals(paymentInfo.getCountryCode(), "UK");
    }

    @Test(groups = "fast")
    public void testSepaMappingServiceWithoutSepaCountryCode() throws Exception {
        SepaDirectDebit paymentInfo = SepaDirectDebitMappingService.toPaymentInfo(accountData, paymentMethodRecord,
                                                                                  ImmutableList.of(new PluginProperty("country", "DE", false)));
        Assert.assertEquals(paymentInfo.getCountryCode(), "DE");
    }

    @Test(groups = "fast")
    public void testSepaMappingServiceWithoutAnyCountryCode() throws Exception {
        when(paymentMethodRecord.getCountry()).thenReturn("DE");
        SepaDirectDebit paymentInfo = SepaDirectDebitMappingService.toPaymentInfo(accountData, paymentMethodRecord, ImmutableList.of());
        Assert.assertEquals(paymentInfo.getCountryCode(), "DE");
    }

    @Test(groups = "fast")
    public void testSepaMappingServiceWithoutAnyCountryCodeAndPaymentMethod() throws Exception {
        when(paymentMethodRecord.getCountry()).thenReturn(null);
        when(accountData.getCountry()).thenReturn("DE");
        SepaDirectDebit paymentInfo = SepaDirectDebitMappingService.toPaymentInfo(accountData, paymentMethodRecord, ImmutableList.of());
        Assert.assertEquals(paymentInfo.getCountryCode(), "DE");
    }

}
