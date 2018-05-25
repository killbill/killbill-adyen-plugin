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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.killbill.adyen.common.Amount;
import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.common.Gender;
import org.killbill.adyen.common.Name;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.ThreeDSecureData;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

public class PaymentRequestBuilder extends RequestBuilder<PaymentRequest> {

    private final String merchantAccount;
    private final PaymentData paymentData;
    private final UserData userData;
    private final SplitSettlementData splitSettlementData;
    private final Map<String, String> additionalData;

    public PaymentRequestBuilder(final String merchantAccount,
                                 final PaymentData paymentData,
                                 final UserData userData,
                                 @Nullable final SplitSettlementData splitSettlementData,
                                 @Nullable final Map<String, String> additionalData,
                                 final PaymentInfoConverterManagement paymentInfoConverterManagement) {
        super(paymentInfoConverterManagement.convertPaymentInfoToPaymentRequest(paymentData.getPaymentInfo()));
        this.merchantAccount = merchantAccount;
        this.paymentData = paymentData;
        this.userData = userData;
        this.splitSettlementData = splitSettlementData;
        this.additionalData = additionalData;
    }

    @Override
    public PaymentRequest build() {
        request.setMerchantAccount(merchantAccount);
        request.setReference(paymentData.getPaymentTransactionExternalKey());

        setAmount();
        setRecurring();
        setShopperData();
        set3DSecureFields();
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
        request.setAmount(amount);
    }

    private void setRecurring() {
        if (paymentData.getPaymentInfo().getContract() != null) {
            final org.killbill.adyen.payment.Recurring recurring = new org.killbill.adyen.payment.Recurring();
            recurring.setContract(paymentData.getPaymentInfo().getContract());
            request.setRecurring(recurring);
        }
    }

    private void setShopperData() {
        final Name name = new Name();
        name.setFirstName(userData.getFirstName());
        name.setInfix(userData.getInfix());
        name.setLastName(userData.getLastName());
        if (userData.getGender() != null) {
            name.setGender(Gender.valueOf(userData.getGender().toUpperCase()));
        }
        if (userData.getFirstName() != null ||
            userData.getInfix() != null ||
            userData.getLastName() != null ||
            userData.getGender() != null) {
            request.setShopperName(name);
        }

        request.setTelephoneNumber(userData.getTelephoneNumber());
        request.setSocialSecurityNumber(userData.getSocialSecurityNumber());
        if (userData.getDateOfBirth() != null) {
            final XMLGregorianCalendar xgc;
            try {
                xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(userData.getDateOfBirth().toGregorianCalendar());
                request.setDateOfBirth(xgc);
            } catch (final DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        request.setShopperEmail(userData.getShopperEmail());
        request.setShopperIP(userData.getShopperIP());
        request.setShopperReference(userData.getShopperReference());
    }

    private void set3DSecureFields() {
        final BigDecimal amount = paymentData.getAmount();
        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();

        boolean thresholdReached = false;
        if (amount != null && paymentInfo.getThreeDThreshold() != null) {
            final Long amountMinorUnits = toMinorUnits(amount, paymentData.getCurrency().name());
            thresholdReached = amountMinorUnits.compareTo(paymentInfo.getThreeDThreshold()) >= 0;
        }
        if (!thresholdReached) {
            return;
        }

        final BrowserInfo browserInfo = new BrowserInfo();
        browserInfo.setAcceptHeader(paymentInfo.getAcceptHeader());
        browserInfo.setUserAgent(paymentInfo.getUserAgent());
        if (browserInfo.getAcceptHeader() != null || browserInfo.getUserAgent() != null) {
            request.setBrowserInfo(browserInfo);
        }

        final ThreeDSecureData threeDSecureData = new ThreeDSecureData();
        threeDSecureData.setDirectoryResponse(paymentInfo.getMpiDataDirectoryResponse());
        threeDSecureData.setAuthenticationResponse(paymentInfo.getMpiDataAuthenticationResponse());
        if (paymentInfo.getMpiDataCavv() != null) {
            threeDSecureData.setCavv(BaseEncoding.base64().encode(paymentInfo.getMpiDataCavv().getBytes(Charsets.US_ASCII)).getBytes(Charsets.US_ASCII));
        }
        threeDSecureData.setCavvAlgorithm(paymentInfo.getMpiDataCavvAlgorithm());
        if (paymentInfo.getMpiDataXid() != null) {
            threeDSecureData.setXid(BaseEncoding.base64().encode(paymentInfo.getMpiDataXid().getBytes(Charsets.US_ASCII)).getBytes(Charsets.US_ASCII));
        }
        threeDSecureData.setEci(paymentInfo.getMpiDataEci());
        if (threeDSecureData.getDirectoryResponse() != null ||
            threeDSecureData.getAuthenticationResponse() != null ||
            threeDSecureData.getCavv() != null ||
            threeDSecureData.getCavvAlgorithm() != null ||
            threeDSecureData.getXid() != null ||
            threeDSecureData.getEci() != null) {
            request.setMpiData(threeDSecureData);
        }

        if (paymentInfo.getTermUrl() != null) {
            addAdditionalDataEntry(request.getAdditionalData().getEntry(), "returnUrl", paymentInfo.getTermUrl());
        }
    }

    private void setSplitSettlementData() {
        if (splitSettlementData != null) {
            final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
            request.getAdditionalData().getEntry().addAll(entries);
        }
    }
}
