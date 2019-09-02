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
import javax.xml.bind.DatatypeConverter;
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
import org.killbill.adyen.threeds2data.ChallengeIndicator;
import org.killbill.adyen.threeds2data.ThreeDS2RequestData;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.BRAND_APPLEPAY;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.BRAND_PAYWITHGOOGLE;

public class PaymentRequestBuilder extends RequestBuilder<PaymentRequest> {

    private final String merchantAccount;
    private final PaymentData paymentData;
    private final UserData userData;
    private final SplitSettlementData splitSettlementData;
    private final Map<String, String> additionalData;

    private static final Logger logger = LoggerFactory.getLogger(PaymentRequestBuilder.class);

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
        setBrowserInfo();
        set3DSFields();
        set3DS2FieldsIfAllowed();
        setSplitSettlementData();
        addAdditionalData();

        return request;
    }

    private void set3DS2FieldsIfAllowed() {
        if(threeDs2Allowed(additionalData)) {
            set3DS2Fields();
        }
    }

    private void addAdditionalData() {
        addAdditionalData(request.getAdditionalData(), additionalData);

        String selectedBrand = paymentData.getPaymentInfo().getSelectedBrand();
        if (BRAND_APPLEPAY.equals(selectedBrand) || BRAND_PAYWITHGOOGLE.equals(selectedBrand)) {
            addAdditionalDataEntry(request.getAdditionalData().getEntry(), "paymentdatasource.type", selectedBrand);
        }

        if (BRAND_PAYWITHGOOGLE.equals(selectedBrand)) {
            boolean isDPAN = paymentData.getPaymentInfo().getMpiDataCavv() != null;
            addAdditionalDataEntry(request.getAdditionalData().getEntry(), "paymentdatasource.tokenized", Boolean.toString(isDPAN));

            if (!isDPAN) {
                addAdditionalDataEntry(request.getAdditionalData().getEntry(), "executeThreeD", "false");
            }
        }
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

    private void setBrowserInfo() {
        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();
        final BrowserInfo browserInfo = new BrowserInfo();
        browserInfo.setAcceptHeader(paymentInfo.getAcceptHeader());
        browserInfo.setColorDepth(paymentInfo.getColorDepth());
        browserInfo.setJavaEnabled(paymentInfo.getJavaEnabled());
        browserInfo.setJavaScriptEnabled(paymentInfo.getJavaScriptEnabled());
        browserInfo.setLanguage(paymentInfo.getBrowserLanguage());
        browserInfo.setScreenHeight(paymentInfo.getScreenHeight());
        browserInfo.setScreenWidth(paymentInfo.getScreenWidth());
        browserInfo.setTimeZoneOffset(paymentInfo.getBrowserTimeZoneOffset());
        browserInfo.setUserAgent(paymentInfo.getUserAgent());
        if (browserInfo.getAcceptHeader() != null || browserInfo.getUserAgent() != null) {
            request.setBrowserInfo(browserInfo);
        }
    }

    private void set3DSFields() {
        final BigDecimal amount = paymentData.getAmount();
        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();

        // applepay and googlepay require mpidata
        String selectedBrand = paymentInfo.getSelectedBrand();
        if (!BRAND_APPLEPAY.equals(selectedBrand) && !BRAND_PAYWITHGOOGLE.equals(selectedBrand)) {
            boolean thresholdReached = false;
            if (amount != null && paymentInfo.getThreeDThreshold() != null) {
                final Long amountMinorUnits = toMinorUnits(amount, paymentData.getCurrency().name());
                thresholdReached = amountMinorUnits.compareTo(paymentInfo.getThreeDThreshold()) >= 0;
            }
            if (!thresholdReached) {
                return;
            }
        }

        final ThreeDSecureData threeDSecureData = new ThreeDSecureData();
        threeDSecureData.setDirectoryResponse(paymentInfo.getMpiDataDirectoryResponse());
        threeDSecureData.setAuthenticationResponse(paymentInfo.getMpiDataAuthenticationResponse());
        threeDSecureData.setCavvAlgorithm(paymentInfo.getMpiDataCavvAlgorithm());
        // Set the unencoded bytes for cavv and xid because JAXB will encode them to base64 automatically when creating
        // a request to Adyen
        threeDSecureData.setCavv(toPlainBytes(paymentInfo.getMpiDataCavv()));
        threeDSecureData.setXid(toPlainBytes(paymentInfo.getMpiDataXid()));
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

    private void set3DS2Fields() {
        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();
        if (paymentInfo.getNotificationUrl() != null) {
            final ThreeDS2RequestData threeDS2RequestData = new ThreeDS2RequestData();
            threeDS2RequestData.setAuthenticationOnly(false);
            threeDS2RequestData.setChallengeIndicator(ChallengeIndicator.NO_PREFERENCE);  // TODO from property
            threeDS2RequestData.setDeviceChannel("browser"); // For channels web and mobile when using webview
            threeDS2RequestData.setNotificationURL(paymentInfo.getNotificationUrl());
            request.setThreeDS2RequestData(threeDS2RequestData);
        }
    }

    private byte[] toPlainBytes(String maybeBase64EncodedString) {
        if (maybeBase64EncodedString != null) {
            byte[] asBytes;

            try {
                return DatatypeConverter.parseBase64Binary(maybeBase64EncodedString);
            }
            catch (IllegalArgumentException ex) {
                return maybeBase64EncodedString.getBytes(Charsets.US_ASCII);
            }
        }
        return null;
    }

    private void setSplitSettlementData() {
        if (splitSettlementData != null) {
            final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
            request.getAdditionalData().getEntry().addAll(entries);
        }
    }
}
