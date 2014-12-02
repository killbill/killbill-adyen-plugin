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

import java.util.List;

import org.killbill.adyen.common.Amount;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;

public class ModificationRequestBuilder extends RequestBuilder<ModificationRequest> {

    public ModificationRequestBuilder() {
        super(new ModificationRequest());
    }

    public ModificationRequestBuilder withOriginalReference(final String value) {
        request.setOriginalReference(value);
        return this;
    }

    public ModificationRequestBuilder withMerchantAccount(final String value) {
        request.setMerchantAccount(value);
        return this;
    }

    public ModificationRequestBuilder withAuthorisationCode(final String value) {
        request.setAuthorisationCode(value);
        return this;
    }

    public ModificationRequestBuilder withAmount(final String currency, final Long value) {
        if (value != null) {
            final Amount amount = new Amount();
            amount.setCurrency(currency);
            amount.setValue(value);
            return withAmount(amount);
        }
        return this;
    }

    public ModificationRequestBuilder withAmount(final Amount amount) {
        request.setModificationAmount(amount);
        return this;
    }

    public ModificationRequestBuilder withSplitSettlementData(final SplitSettlementData splitSettlementData) {
        final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
        addAdditionalData(entries);
        return this;
    }

    @Override
    protected List<AnyType2AnyTypeMap.Entry> getAdditionalData() {
        if (request.getAdditionalData() == null) {
            request.setAdditionalData(new AnyType2AnyTypeMap());
        }
        return request.getAdditionalData().getEntry();
    }
}
