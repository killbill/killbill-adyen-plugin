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

package org.killbill.billing.plugin.adyen.client.payment.converter.impl;

import java.util.List;

import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

public class PaymentInfoConverterService implements PaymentInfoConverterManagement<PaymentInfo> {

    private final List<PaymentInfoConverter<? extends PaymentInfo>> paymentInfoConverters;

    public PaymentInfoConverterService() {
        this(ImmutableList.<PaymentInfoConverter<? extends PaymentInfo>>of(
                new AmexConverter(),
                new DinersConverter(),
                new CreditCardConverter(),
                new ElvConverter(),
                new MaestroConverter(),
                new MaestroUKConverter(),
                new MasterCardConverter(),
                new VisaConverter(),
                new SepaDirectDebitConverter(),
                new RecurringConverter()));
    }

    public PaymentInfoConverterService(final List<PaymentInfoConverter<? extends PaymentInfo>> paymentInfoConverterList) {
        Preconditions.checkArgument(paymentInfoConverterList != null, "paymentInfoConverterList is null");
        this.paymentInfoConverters = paymentInfoConverterList;
    }

    @Override
    public Object getBrowserInfoFor3DSecureAuth(final Long billedAmount, final Card card) {
        checkNotNull(billedAmount, "billedAmount is null");
        checkNotNull(card, "card is null");

        for (final PaymentInfoConverter pic : paymentInfoConverters) {
            if (pic.getPaymentType().equals(card.getPaymentProvider().getPaymentType())) {
                return pic.convertPaymentInfoFor3DSecureAuth(billedAmount, card);
            }
        }
        throw new IllegalArgumentException("No PaymentInfoConverter for " + card.getPaymentProvider().getPaymentType() + " found.");
    }

    @Override
    public PaymentInfoConverter<PaymentInfo> getConverterForPaymentInfo(final PaymentInfo paymentInfo) {
        for (final PaymentInfoConverter pic : paymentInfoConverters) {
            if (pic.getPaymentType().equals(paymentInfo.getPaymentProvider().getPaymentType())) {
                return pic;
            }
        }
        throw new IllegalArgumentException("No PaymentInfoConverter for " + paymentInfo.getPaymentProvider().getPaymentType() + " found.");
    }
}
