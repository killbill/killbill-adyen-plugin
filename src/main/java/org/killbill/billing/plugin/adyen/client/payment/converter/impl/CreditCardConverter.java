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

import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;

public class CreditCardConverter extends PaymentInfoConverter<Card> {

    @Override
    public boolean supportsPaymentInfo(final PaymentInfo paymentInfo) {
        return paymentInfo instanceof Card;
    }

    @Override
    public PaymentRequest convertPaymentInfoToPaymentRequest(final Card paymentInfo) {
        final org.killbill.adyen.payment.Card card = new org.killbill.adyen.payment.Card();
        card.setNumber(paymentInfo.getNumber());
        card.setHolderName(paymentInfo.getHolderName());
        card.setCvc(paymentInfo.getCvc());
        if (paymentInfo.getExpiryMonth() != null) {
            card.setExpiryMonth(paymentInfo.getExpiryMonth().toString());
        }
        if (paymentInfo.getExpiryYear() != null) {
            card.setExpiryYear(paymentInfo.getExpiryYear().toString());
        }

        final PaymentRequest adyenRequest = super.convertPaymentInfoToPaymentRequest(paymentInfo);
        adyenRequest.setCard(card);
        adyenRequest.setBillingAddress(adyenRequest.getBillingAddress());
        setAdditionalData(paymentInfo, adyenRequest);

        return adyenRequest;
    }

    private void setAdditionalData(final Card paymentInfo, final PaymentRequest paymentRequest) {
        // For DineroMail, this option must be set to provide the issuer country of the card/the country of the shopper
        if (paymentInfo.getIssuerCountry() != null) {
            final AnyType2AnyTypeMap.Entry issuerCountry = new AnyType2AnyTypeMap.Entry();
            issuerCountry.setKey("issuerCountry");
            issuerCountry.setValue(paymentInfo.getIssuerCountry());
            paymentRequest.getAdditionalData().getEntry().add(issuerCountry);
        }

        // Apple Pay
        if (paymentInfo.getToken() != null) {
            final AnyType2AnyTypeMap.Entry paymentToken = new AnyType2AnyTypeMap.Entry();
            paymentToken.setKey("payment.token");
            paymentToken.setValue(paymentInfo.getToken());
            paymentRequest.getAdditionalData().getEntry().add(paymentToken);
        }

        // Easy encryption
        if (paymentInfo.getEncryptedJson() != null) {
            final AnyType2AnyTypeMap.Entry cardEncryptedJson = new AnyType2AnyTypeMap.Entry();
            cardEncryptedJson.setKey("card.encrypted.json");
            cardEncryptedJson.setValue(paymentInfo.getEncryptedJson());
            paymentRequest.getAdditionalData().getEntry().add(cardEncryptedJson);
        }
    }
}
