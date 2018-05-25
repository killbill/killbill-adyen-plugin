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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.killbill.adyen.payment.AnyType2AnyTypeMap.Entry;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class TestModificationRequestBuilder extends BaseTestPaymentRequestBuilder {

    @Test(groups = "fast")
    public void testPaymentRequestBuilderWithoutSplitSettlements() {
        final SplitSettlementData splitSettlementData = null;

        verifyModificationRequestBuilder(splitSettlementData);
    }

    @Test(groups = "fast")
    public void testPaymentRequestBuilderWithSplitSettlements() {
        final SplitSettlementData splitSettlementData = new SplitSettlementData(1,
                                                                                "EUR",
                                                                                ImmutableList.<SplitSettlementData.Item>of(new SplitSettlementData.Item(500, "deal1", "voucherId", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal1", "voucherId2", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal2", "travelId", "travel")));

        final ModificationRequest modificationRequest = verifyModificationRequestBuilder(splitSettlementData);

        final List<Entry> entries = modificationRequest.getAdditionalData().getEntry();
        Assert.assertEquals(findValue(entries, "splitsettlementdata.api"), "1");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.nrOfItems"), "3");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.totalAmount"), "2000");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.currencyCode"), "EUR");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.amount"), "500");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.currencyCode"), "EUR");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.group"), "deal1");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.reference"), "voucherId");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item1.type"), "voucher");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.amount"), "750");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.currencyCode"), "EUR");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.group"), "deal1");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.reference"), "voucherId2");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item2.type"), "voucher");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.amount"), "750");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.currencyCode"), "EUR");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.group"), "deal2");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.reference"), "travelId");
        Assert.assertEquals(findValue(entries, "splitsettlementdata.item3.type"), "travel");
    }

    private ModificationRequest verifyModificationRequestBuilder(final SplitSettlementData splitSettlementData) {
        final String merchantAccount = UUID.randomUUID().toString();
        final String paymentTransactionExternalKey = UUID.randomUUID().toString();
        final PaymentData paymentData = new PaymentData<Card>(new BigDecimal("20"), Currency.EUR, paymentTransactionExternalKey, new Card());
        final String originalReference = UUID.randomUUID().toString();
        final Map<String, String> additionalData = null;

        final ModificationRequestBuilder builder = new ModificationRequestBuilder(merchantAccount, paymentData, originalReference, splitSettlementData, additionalData);
        final ModificationRequest modificationRequest = builder.build();

        Assert.assertEquals(modificationRequest.getMerchantAccount(), merchantAccount);
        Assert.assertEquals(modificationRequest.getModificationAmount().getValue(), (Long) 2000L);
        Assert.assertEquals(modificationRequest.getOriginalReference(), originalReference);
        Assert.assertEquals(modificationRequest.getReference(), paymentTransactionExternalKey);

        return modificationRequest;
    }
}
