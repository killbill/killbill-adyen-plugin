/*
 * Copyright 2015 Groupon, Inc
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

import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.MaestroUK;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;

public class MaestroUKConverter implements PaymentInfoConverter<MaestroUK> {

    @Override
    public Object convertPaymentInfoToPSPTransferObject(final String holderName, final MaestroUK paymentInfo) {
        final PaymentRequest request = new PaymentRequest();
        org.killbill.adyen.payment.Card card = null;
        if (paymentInfo.getRecurringDetailId() == null || paymentInfo.getRecurringDetailId().isEmpty()) {
            card = new org.killbill.adyen.payment.Card();
            card.setNumber(paymentInfo.getCcNumber());
            if (paymentInfo.getCcHolderName() != null && !paymentInfo.getCcHolderName().isEmpty()) {
                card.setHolderName(paymentInfo.getCcHolderName());
            } else {
                card.setHolderName(paymentInfo.getCcHolderName());
            }
            card.setCvc(paymentInfo.getCcSecCode());
            card.setExpiryMonth(String.valueOf(paymentInfo.getValidUntilMonth()));
            card.setExpiryYear(String.valueOf(paymentInfo.getValidUntilYear()));
            if (paymentInfo.getCcIssueNumber() != null && !paymentInfo.getCcIssueNumber().isEmpty()) {
                card.setIssueNumber(paymentInfo.getCcIssueNumber());
            }
            card.setStartMonth(String.valueOf(paymentInfo.getValidStartMonth()));
            card.setStartYear(String.valueOf(paymentInfo.getValidStartYear()));
        }
        request.setCard(card);
        return request;
    }

    @Override
    public Object convertPaymentInfoFor3DSecureAuth(final Long billedAmount, final Card card) {
        final BrowserInfo info = new BrowserInfo();
        info.setAcceptHeader(card.getAcceptHeader());
        info.setUserAgent(card.getUserAgent());
        return info;
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.MAESTROUK;
    }
}
