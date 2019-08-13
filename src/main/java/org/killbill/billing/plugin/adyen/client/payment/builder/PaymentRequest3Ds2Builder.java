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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import org.killbill.adyen.common.*;
import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.adyen.payment.PaymentRequest3Ds2;
import org.killbill.adyen.payment.Recurring;
import org.killbill.adyen.threeds2data.ChallengeIndicator;
import org.killbill.adyen.threeds2data.ThreeDS2RequestData;
import org.killbill.adyen.threeds2data.ThreeDS2Result;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;

import javax.annotation.Nullable;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.Map;

public class PaymentRequest3Ds2Builder extends RequestBuilder<PaymentRequest3Ds2> {

    private final String merchantAccount;
    private final PaymentData paymentData;
    private final UserData userData;
    private final SplitSettlementData splitSettlementData;
    private final Map<String, String> additionalData;

    public PaymentRequest3Ds2Builder(final String merchantAccount,
                                     final PaymentData paymentData,
                                     final UserData userData,
                                     @Nullable final SplitSettlementData splitSettlementData,
                                     @Nullable final Map<String, String> additionalData) {
        super(new PaymentRequest3Ds2());
        final AnyType2AnyTypeMap map = new AnyType2AnyTypeMap();
        request.setAdditionalData(map);

        this.merchantAccount = merchantAccount;
        this.paymentData = paymentData;
        this.userData = userData;
        this.splitSettlementData = splitSettlementData;
        this.additionalData = additionalData;
    }

    @Override
    public PaymentRequest3Ds2 build() {
        request.setMerchantAccount(merchantAccount);
        request.setReference(paymentData.getPaymentTransactionExternalKey());
        request.setSelectedBrand(paymentData.getPaymentInfo().getSelectedBrand());

        // TODO: values that not sent yet
        // * accountInfo
        // * deviceFingerprint
        // * merchantRiskIndicator
        // * merchantOrderReference
        setAmount();
        setBillingAddress();
        setRecurring();
        setShopperData();
        set3DS2FieldsIfAllowed();
        setBrowserInfo();
        setSplitSettlementData();
        addAdditionalData(request.getAdditionalData(), additionalData);

        return request;
    }

    private void set3DS2FieldsIfAllowed() {
        if(threeDs2Allowed(additionalData)) {
            set3DS2Fields();
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

    private void setBillingAddress() {
        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();
        final Address billingAddress = new Address();

        billingAddress.setCity(paymentInfo.getCity());
        billingAddress.setCountry(paymentInfo.getCountry());
        billingAddress.setHouseNumberOrName(paymentInfo.getHouseNumberOrName());
        billingAddress.setPostalCode(paymentInfo.getPostalCode());
        billingAddress.setStateOrProvince(paymentInfo.getStateOrProvince());
        billingAddress.setStreet(paymentInfo.getStreet());
        request.setBillingAddress(billingAddress);
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

    private void setRecurring() {
        if (paymentData.getPaymentInfo() instanceof org.killbill.billing.plugin.adyen.client.model.paymentinfo.Recurring) {
            final Recurring recurring = new Recurring();
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

    private void set3DS2Fields() {
        final PaymentInfo paymentInfo = paymentData.getPaymentInfo();
        final ThreeDS2RequestData threeDS2RequestData = new ThreeDS2RequestData();

        if (paymentInfo.getTransStatus() != null) {
            final ThreeDS2Result threeDS2Result = new ThreeDS2Result();
            threeDS2Result.setTransStatus(paymentInfo.getTransStatus());
            threeDS2Result.setThreeDSServerTransID(paymentInfo.getThreeDSServerTransID());
            request.setThreeDS2Result(threeDS2Result);
        }
        else {
            threeDS2RequestData.setAuthenticationOnly(false);
            threeDS2RequestData.setChallengeIndicator(ChallengeIndicator.NO_PREFERENCE);  // TODO from property
            threeDS2RequestData.setDeviceChannel("browser");
            threeDS2RequestData.setNotificationURL(paymentInfo.getNotificationUrl());
            threeDS2RequestData.setThreeDSCompInd(paymentInfo.getThreeDSCompInd());
            threeDS2RequestData.setThreeDSServerTransID(paymentInfo.getThreeDSServerTransID());
            threeDS2RequestData.setMessageVersion(paymentInfo.getMessageVersion());
            request.setThreeDS2RequestData(threeDS2RequestData);
        }
        request.setThreeDS2Token(paymentInfo.getThreeDS2Token());
    }

    private void setSplitSettlementData() {
        if (splitSettlementData != null) {
            final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
            request.getAdditionalData().getEntry().addAll(entries);
        }
    }
}
