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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.AnyType2AnyTypeMap.Entry;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class SplitSettlementParamsBuilder {

    private static final String SPLITSETTLEMENT = "splitsettlementdata";
    private static final Joiner JOINER = Joiner.on(":");

    public Map<String, String> createSignedParamsFrom(final SplitSettlementData splitSettlementData,
                                                      final String merchantSignature,
                                                      final Signer signer,
                                                      final String secret,
                                                      final String algorithm) throws SignatureGenerationException {
        final Map<String, String> params = toMap(createEntriesFrom(splitSettlementData));

        if ("HmacSHA1".equals(algorithm)) {
            final SortedMap<String, String> sortedParams = new TreeMap<String, String>(params);
            sortedParams.put("merchantSig", merchantSignature);
            final String concatenatedKeys = JOINER.join(sortedParams.keySet());
            final String concatenatedValues = JOINER.join(sortedParams.values());
            final String signature = signer.signData(secret, algorithm, concatenatedKeys + "|" + concatenatedValues);

            params.put(SPLITSETTLEMENT + ".sig", signature);
        }

        return params;
    }

    public List<AnyType2AnyTypeMap.Entry> createEntriesFrom(final SplitSettlementData splitSettlementData) {
        final List<AnyType2AnyTypeMap.Entry> entries = new ArrayList<AnyType2AnyTypeMap.Entry>();
        if (splitSettlementData != null && !splitSettlementData.getItems().isEmpty()) {
            final String prefix = SPLITSETTLEMENT + ".";
            entries.addAll(ImmutableList.<AnyType2AnyTypeMap.Entry>of(stringEntry(prefix + "api", splitSettlementData.getApi()),
                                                                      stringEntry(prefix + "nrOfItems", splitSettlementData.getItems().size()),
                                                                      stringEntry(prefix + "totalAmount", splitSettlementData.getTotalAmount()),
                                                                      stringEntry(prefix + "currencyCode", splitSettlementData.getCurrencyCode())));

            for (int idx = 1; idx <= splitSettlementData.getItems().size(); idx++) {
                entries.addAll(itemEntries(idx, splitSettlementData.getItems().get(idx - 1), splitSettlementData.getCurrencyCode()));
            }
        }
        return entries;
    }

    private Collection<Entry> itemEntries(final int idx, final SplitSettlementData.Item item, final String currencyCode) {
        final String prefix = SPLITSETTLEMENT + ".item" + idx + ".";
        return ImmutableList.<AnyType2AnyTypeMap.Entry>of(stringEntry(prefix + "amount", item.getAmount()),
                                                          stringEntry(prefix + "currencyCode", currencyCode),
                                                          stringEntry(prefix + "group", item.getGroup()),
                                                          stringEntry(prefix + "reference", item.getReference()),
                                                          stringEntry(prefix + "type", item.getType()));
    }

    private AnyType2AnyTypeMap.Entry stringEntry(final Object key, final Object value) {
        final AnyType2AnyTypeMap.Entry entry = new AnyType2AnyTypeMap.Entry();
        entry.setKey(String.valueOf(key));
        entry.setValue(String.valueOf(value));
        return entry;
    }

    private Map<String, String> toMap(final Iterable<Entry> entries) {
        final Map<String, String> map = new HashMap<String, String>();
        for (final AnyType2AnyTypeMap.Entry entry : entries) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return map;
    }
}
