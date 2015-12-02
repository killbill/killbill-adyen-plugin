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
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.OneClick;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Recurring;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.SepaDirectDebit;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;

import com.google.common.base.Strings;

import static org.killbill.billing.plugin.adyen.client.model.PaymentType.EMPTY;
import static org.killbill.billing.plugin.adyen.client.model.RecurringType.ONECLICK;

public class RecurringConverter implements PaymentInfoConverter<Recurring> {

    @Override
    public Object convertPaymentInfoToPSPTransferObject(final String holderName, final Recurring recurring) {
        final PaymentRequest adyenRequest = new PaymentRequest();
        addInstallments(recurring, adyenRequest);
        setCvcForOneClick(recurring, adyenRequest);
        convertElvRecurringToSepa(recurring, adyenRequest);
        return adyenRequest;
    }

    private void addInstallments(Recurring recurring, PaymentRequest adyenRequest) {
        AnyType2AnyTypeMap map = new AnyType2AnyTypeMap();
        if (recurring.getInstallments() != null && recurring.getInstallments() > 1) {
            AnyType2AnyTypeMap.Entry installments = new AnyType2AnyTypeMap.Entry();
            installments.setKey("installments");
            installments.setValue(recurring.getInstallments().toString());
            map.getEntry().add(installments);
            adyenRequest.setAdditionalData(map);
        }
    }

    private void setCvcForOneClick(Recurring recurring, PaymentRequest adyenRequest) {
        if (recurring.getRecurringType() == ONECLICK) {
            org.killbill.adyen.payment.Card card = new org.killbill.adyen.payment.Card();
            card.setCvc(((OneClick) recurring).getCcSecCode());
            adyenRequest.setCard(card);
        }
    }

    void convertElvRecurringToSepa(Recurring recurring, PaymentRequest paymentRequest) {
        if (elvRecurring(recurring)) {
            paymentRequest.setSelectedBrand(SepaDirectDebit.SELECTED_BRAND);
        }
    }

    private boolean elvRecurring(Recurring recurring) {
        return PaymentType.ELV.equals(recurring.getPaymentProvider().getPaymentType());
    }

    @Override
    public Object convertPaymentInfoFor3DSecureAuth(final Long billedAmount, final Card card) {
        final boolean thresholdReached = billedAmount >= card.getPaymentProvider().getThreeDThreshold();
        final String acceptHeader = card.getAcceptHeader();
        final String userAgent = card.getUserAgent();
        BrowserInfo browserInfo = null;
        if (thresholdReached && !Strings.isNullOrEmpty(acceptHeader) && !Strings.isNullOrEmpty(userAgent)) {
            browserInfo = new BrowserInfo();
            browserInfo.setAcceptHeader(acceptHeader);
            browserInfo.setUserAgent(userAgent);
        }
        return browserInfo;
    }

    @Override
    public boolean supportsPaymentType(final PaymentType type) {
        return EMPTY.equals(type);
    }

}
