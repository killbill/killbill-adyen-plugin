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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.AnyType2AnyTypeMap.Entry;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class TestSplitSettlementParamsBuilder {

    @Test(groups = "fast")
    public void createSignedParams() throws SignatureGenerationException {
        final SplitSettlementData splitSettlementData = new SplitSettlementData(1,
                                                                                "EUR",
                                                                                ImmutableList.<SplitSettlementData.Item>of(new SplitSettlementData.Item(500, "deal1", "voucherId", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal1", "voucherId2", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal2", "travelId", "travel")));
        final String merchantSignature = "A8fZw3UV2aLfWtr8mrqy8+wtVXs=";
        final Signer signer = new Signer();
        final String secret = "Hello kitty";
        final String algorithm = AdyenConfigProperties.DEFAULT_HMAC_ALGORITHM;

        final Map<String, String> signedParams = new SplitSettlementParamsBuilder().createSignedParamsFrom(splitSettlementData, merchantSignature, signer, secret, algorithm);

        Assert.assertEquals("fTERx5eYhC8UWEJplsh+H89/DPsLC9ZXNLKo0fCluLg=", signedParams.get("splitsettlementdata.sig"));
    }

    @Test(groups = "fast")
    public void createEntriesFromNull() {
        final List<Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(null);
        Assert.assertTrue(entries.isEmpty());
    }

    @Test(groups = "fast")
    public void createEntriesFromSplitSettlementData() {
        final SplitSettlementData splitSettlementData = new SplitSettlementData(1,
                                                                                "EUR",
                                                                                ImmutableList.<SplitSettlementData.Item>of(new SplitSettlementData.Item(500, "deal1", "voucherId", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal1", "voucherId2", "voucher"),
                                                                                                                           new SplitSettlementData.Item(750, "deal2", "travelId", "travel")));

        final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);

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
        Assert.assertEquals(entries.size(), 19);
    }

    private Object findValue(final Collection<Entry> entries, final Object key) {
        for (final AnyType2AnyTypeMap.Entry entry : entries) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
