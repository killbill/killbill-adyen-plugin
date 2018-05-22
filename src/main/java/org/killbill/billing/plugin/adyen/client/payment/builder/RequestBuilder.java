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
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.AnyType2AnyTypeMap.Entry;
import org.killbill.billing.plugin.util.KillBillMoney;

public abstract class RequestBuilder<R> {

    protected R request;

    protected RequestBuilder(final R request) {
        this.request = request;
    }

    public R build() {
        return request;
    }

    protected void addAdditionalDataEntry(final Collection<Entry> entries, final String key, final String value) {
        final AnyType2AnyTypeMap.Entry entry = new AnyType2AnyTypeMap.Entry();
        entry.setKey(key);
        entry.setValue(value);
        entries.add(entry);
    }

    protected Long toMinorUnits(@Nullable final BigDecimal amountBD, @Nullable final String currencyIsoCode) {
        if (amountBD == null || currencyIsoCode == null) {
            return null;
        }
        return KillBillMoney.toMinorUnits(currencyIsoCode, amountBD);
    }

    protected void addAdditionalData(final AnyType2AnyTypeMap additionalDataEntries, @Nullable final Map<String, String> additionalData) {
        if (additionalData != null) {
            for (final String key : additionalData.keySet()) {
                addAdditionalDataEntry(additionalDataEntries.getEntry(), key, additionalData.get(key));
            }
        }
    }
}
