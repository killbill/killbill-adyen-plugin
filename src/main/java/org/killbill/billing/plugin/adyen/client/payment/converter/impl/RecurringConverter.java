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

import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Recurring;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;

public class RecurringConverter extends PaymentInfoConverter<Recurring> {

    @Override
    public boolean supportsPaymentInfo(final PaymentInfo paymentInfo) {
        return paymentInfo instanceof Recurring;
    }

    @Override
    public PaymentRequest convertPaymentInfoToPaymentRequest(final Recurring paymentInfo) {
        final PaymentRequest paymentRequest = super.convertPaymentInfoToPaymentRequest(paymentInfo);

        paymentRequest.setSelectedRecurringDetailReference(paymentInfo.getRecurringDetailReference());
        setCvcForOneClick(paymentInfo, paymentRequest);

        return paymentRequest;
    }

    private void setCvcForOneClick(final Recurring paymentInfo, final PaymentRequest paymentRequest) {
        if (paymentInfo.getCvc() != null) {
            final org.killbill.adyen.payment.Card card = new org.killbill.adyen.payment.Card();
            card.setCvc(paymentInfo.getCvc());
            paymentRequest.setCard(card);
        }
    }
}
