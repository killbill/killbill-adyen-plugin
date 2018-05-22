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

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.adyen.common.Amount;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.ModificationRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;

public class ModificationRequestBuilder extends RequestBuilder<ModificationRequest> {

    private final String merchantAccount;
    private final PaymentData paymentData;
    private final String originalReference;
    private final SplitSettlementData splitSettlementData;
    private final Map<String, String> additionalData;

    public ModificationRequestBuilder(final String merchantAccount,
                                      final PaymentData paymentData,
                                      final String originalReference,
                                      @Nullable final SplitSettlementData splitSettlementData,
                                      @Nullable final Map<String, String> additionalData) {
        super(new ModificationRequest());
        final AnyType2AnyTypeMap map = new AnyType2AnyTypeMap();
        request.setAdditionalData(map);

        this.merchantAccount = merchantAccount;
        this.paymentData = paymentData;
        this.originalReference = originalReference;
        this.splitSettlementData = splitSettlementData;
        this.additionalData = additionalData;
    }

    @Override
    public ModificationRequest build() {
        request.setMerchantAccount(merchantAccount);
        request.setOriginalReference(originalReference);
        request.setReference(paymentData.getPaymentTransactionExternalKey());

        setAmount();
        setSplitSettlementData();
        addAdditionalData(request.getAdditionalData(), additionalData);

        return request;
    }

    private void setAmount() {
        if (paymentData.getAmount() == null || paymentData.getCurrency() == null) {
            return;
        }

        final String currency = paymentData.getCurrency().name();
        final Amount amount = new Amount();
        amount.setValue(toMinorUnits(paymentData.getAmount(), currency));
        amount.setCurrency(currency);
        request.setModificationAmount(amount);
    }

    private void setSplitSettlementData() {
        if (splitSettlementData != null) {
            final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
            request.getAdditionalData().getEntry().addAll(entries);
        }
    }
}
