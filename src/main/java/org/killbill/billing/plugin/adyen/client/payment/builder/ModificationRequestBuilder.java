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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.util.List;

import javax.annotation.Nullable;

import org.killbill.adyen.common.Amount;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;

public class ModificationRequestBuilder extends RequestBuilder<ModificationRequest> {

    public ModificationRequestBuilder(final String merchantAccount,
                                      final String originalReference,
                                      final String reference) {
        this(merchantAccount, null, null, originalReference, reference);
    }

    public ModificationRequestBuilder(final String merchantAccount,
                                      @Nullable final Long amountL,
                                      final String currency,
                                      final String originalReference,
                                      final String reference) {
        super(new ModificationRequest());

        request.setMerchantAccount(merchantAccount);
        if (amountL != null) {
            final Amount amount = new Amount();
            amount.setValue(amountL);
            amount.setCurrency(currency);
            request.setModificationAmount(amount);
        }
        request.setOriginalReference(originalReference);
        request.setReference(reference);
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
