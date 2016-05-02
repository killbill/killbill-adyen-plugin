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

package org.killbill.billing.plugin.adyen.client.payment.converter.impl;

import java.util.List;

import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;

import com.google.common.collect.ImmutableList;

public class PaymentInfoConverterService implements PaymentInfoConverterManagement<PaymentInfo> {

    private final List<PaymentInfoConverter<? extends PaymentInfo>> paymentInfoConverters;

    public PaymentInfoConverterService() {
        this.paymentInfoConverters = ImmutableList.<PaymentInfoConverter<? extends PaymentInfo>>of(new CreditCardConverter(),
                                                                                                   new SepaDirectDebitConverter(),
                                                                                                   new RecurringConverter(),
                                                                                                   // Default fallback
                                                                                                   new PaymentInfoConverter<PaymentInfo>());
    }

    @Override
    public PaymentRequest convertPaymentInfoToPaymentRequest(final PaymentInfo paymentInfo) {
        for (final PaymentInfoConverter pic : paymentInfoConverters) {
            if (pic.supportsPaymentInfo(paymentInfo)) {
                return pic.convertPaymentInfoToPaymentRequest(paymentInfo);
            }
        }
        // Should never happen
        throw new IllegalArgumentException("No PaymentInfoConverter for " + paymentInfo + " found.");
    }
}
