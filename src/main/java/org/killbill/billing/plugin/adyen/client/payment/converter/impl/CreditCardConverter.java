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

import com.google.common.collect.ImmutableSet;
import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.billing.plugin.adyen.client.model.Acquirer;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.RecurringType;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.CreditCard;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverter;

import com.google.common.base.Strings;

import java.util.Set;

import static org.killbill.billing.plugin.adyen.client.model.PaymentType.*;

public class CreditCardConverter implements PaymentInfoConverter<Card> {

    private static final Set<PaymentType> SUPPORTED_PAYMENT_TYPES = ImmutableSet.<PaymentType>builder()
                                                                                .add(CREDITCARD)
                                                                                .add(MASTERCARD)
                                                                                .add(VISA)
                                                                                .add(AMEX)
                                                                                .add(DINERSCLUB)
                                                                                .add(ELO)
                                                                                .add(DANKORT)
                                                                                .add(SHOPPING)
                                                                                .add(CABAL)
                                                                                .build();

    @Override
    public Object convertPaymentInfoToPSPTransferObject(final String holderName, final Card paymentInfo) {
        final PaymentProvider paymentProvider = paymentInfo.getPaymentProvider();
        final PaymentRequest adyenRequest = new PaymentRequest();
        org.killbill.adyen.payment.Card card = null;
        final AnyType2AnyTypeMap map = new AnyType2AnyTypeMap();
        if (paymentInfo.getRecurringDetailId() == null || paymentInfo.getRecurringDetailId().isEmpty()) {
            card = new org.killbill.adyen.payment.Card();
            card.setNumber(paymentInfo.getCcNumber());
            if (paymentInfo.getCcHolderName() != null && !paymentInfo.getCcHolderName().isEmpty()) {
                card.setHolderName(paymentInfo.getCcHolderName());
            } else {
                card.setHolderName(holderName);
            }
            if (getCcSecCode(paymentInfo) != null) {
                card.setCvc(getCcSecCode(paymentInfo));
            }
            card.setExpiryMonth(String.valueOf(paymentInfo.getValidUntilMonth()));
            card.setExpiryYear(String.valueOf(paymentInfo.getValidUntilYear()));
            // Set the concrete card brand when using subBrand types (for dinero)
            if (paymentProvider.getPaymentServiceProvider() != null && !paymentProvider.getPaymentServiceProvider().getSubBrand().isEmpty()) {
                adyenRequest.setSelectedBrand(paymentInfo.getPaymentProvider().getPaymentType().getName());
                final AnyType2AnyTypeMap.Entry overwriteBrand = new AnyType2AnyTypeMap.Entry();
                overwriteBrand.setKey("overwriteBrand");
                overwriteBrand.setValue("true");
                map.getEntry().add(overwriteBrand);
                final AnyType2AnyTypeMap.Entry issuerCountry = new AnyType2AnyTypeMap.Entry();
                issuerCountry.setKey("issuerCountry");
                issuerCountry.setValue(paymentProvider.getCountryIsoCode());
                map.getEntry().add(issuerCountry);
            }
        } else if (paymentInfo.getPaymentProvider().getRecurringType() == RecurringType.ONECLICK) {
            card = new org.killbill.adyen.payment.Card();
            card.setCvc(getCcSecCode(paymentInfo));
        }

        if (getInstallments(paymentInfo) != null && getInstallments(paymentInfo) > 1) {
            final AnyType2AnyTypeMap.Entry installments = new AnyType2AnyTypeMap.Entry();
            installments.setKey("installments");
            installments.setValue(getInstallments(paymentInfo).toString());
            map.getEntry().add(installments);
        }

        // Add the acquirer to the request, if one is set
        final String acquirerName = paymentInfo.getAcquirer();
        final Acquirer defaultAcquirer = paymentProvider.getDefaultAcquirer();
        final Acquirer paymentInfoAcquirer = paymentProvider.getAcquirerByName(acquirerName);
        final Acquirer acquirer = paymentInfoAcquirer == null ? defaultAcquirer : paymentInfoAcquirer;
        if (acquirer != null) {
            final AnyType2AnyTypeMap.Entry acquirerCode = new AnyType2AnyTypeMap.Entry();
            acquirerCode.setKey("acquirerCode");
            acquirerCode.setValue(acquirer.getName());
            map.getEntry().add(acquirerCode);
            // If the acquirer has an authorisationMid set it to the request too
            final String mid = acquirer.getMid();
            if (!Strings.isNullOrEmpty(mid)) {
                final AnyType2AnyTypeMap.Entry authorisationMid = new AnyType2AnyTypeMap.Entry();
                authorisationMid.setKey("authorisationMid");
                authorisationMid.setValue(mid);
                map.getEntry().add(authorisationMid);
            }
        }
        if (!map.getEntry().isEmpty()) {
            adyenRequest.setAdditionalData(map);
        }

        adyenRequest.setCard(card);
        return adyenRequest;
    }

    @Override
    public Object convertPaymentInfoFor3DSecureAuth(final Long billedAmount, final Card card) {
        final Long threeDThreshold = card.getPaymentProvider().getThreeDThreshold();
        final boolean thresholdReached = billedAmount.compareTo(threeDThreshold) >= 0;
        final String acceptHeader = card.getAcceptHeader();
        final String userAgent = card.getUserAgent();
        BrowserInfo result = null;
        if (thresholdReached && !Strings.isNullOrEmpty(acceptHeader) && !Strings.isNullOrEmpty(userAgent)) {
            result = new BrowserInfo();
            result.setAcceptHeader(acceptHeader);
            result.setUserAgent(userAgent);
        }
        return result;
    }

    @Override
    public boolean supportsPaymentType(final PaymentType type) {
        return type != null && SUPPORTED_PAYMENT_TYPES.contains(type);
    }

    public String getCcSecCode(final Card card) {
        if (card instanceof CreditCard) {
            return card.getCcSecCode();
        }
        return null;
    }

    public Integer getInstallments(final Card card) {
        if (card instanceof CreditCard) {
            return ((CreditCard) card).getInstallments();
        }
        return null;
    }
}
