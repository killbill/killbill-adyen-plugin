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

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.WebPaymentFrontend;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.service.PayPalCountryCodes;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

public class HPPRequestBuilder extends RequestBuilder<Map<String, String>> {

    private static final Locale LOCALE_EN_UK = new Locale("en", "UK");

    private final String merchantAccount;
    private final PaymentData paymentData;
    private final UserData userData;
    private final SplitSettlementData splitSettlementData;
    private final AdyenConfigProperties adyenConfigProperties;
    private final Signer signer;

    public HPPRequestBuilder(final String merchantAccount,
                             final PaymentData paymentData,
                             final UserData userData,
                             @Nullable final SplitSettlementData splitSettlementData,
                             final AdyenConfigProperties adyenConfigProperties,
                             final Signer signer) {
        super(new TreeMap<String, String>());
        this.merchantAccount = merchantAccount;
        this.paymentData = paymentData;
        this.userData = userData;
        this.splitSettlementData = splitSettlementData;
        this.adyenConfigProperties = adyenConfigProperties;
        this.signer = signer;
    }

    @Override
    public Map<String, String> build() {
        request.put("merchantAccount", merchantAccount);
        request.put("merchantReference", paymentData.getPaymentTransactionExternalKey());

        final String currency = paymentData.getCurrency().name();
        final Long amount = toMinorUnits(paymentData.getAmount(), currency);
        request.put("paymentAmount", amount.toString());
        request.put("currencyCode", currency);

        final WebPaymentFrontend paymentInfo = (WebPaymentFrontend) paymentData.getPaymentInfo();

        setShopperData();

        // NOTE: Locale.UK is defined as "en_GB".
        Locale shopperLocale = Locale.UK.equals(userData.getShopperLocale()) ? LOCALE_EN_UK : userData.getShopperLocale();
        if (shopperLocale != null) {
            if ("paypal".equalsIgnoreCase(paymentInfo.getBrandCode()) &&
                PayPalCountryCodes.isNotPayPalIsoCode(shopperLocale.getCountry()) &&
                PayPalCountryCodes.isNotPayPalLocale(userData.getShopperLocale())) {
                shopperLocale = Locale.US;
            }
            request.put("shopperLocale", shopperLocale.toString());
        }
        request.put("countryCode", paymentInfo.getCountry());
        request.put("shipBeforeDate", paymentInfo.getShipBeforeDate());
        request.put("skinCode", paymentInfo.getSkinCode());
        request.put("sessionValidity", paymentInfo.getSessionValidity());
        request.put("resURL", paymentInfo.getResURL());
        request.put("brandCode", paymentInfo.getBrandCode());
        request.put("allowedMethods", paymentInfo.getAllowedMethods());

        request.put("recurringContract", paymentInfo.getContract());

        final String hmacSecret = adyenConfigProperties.getHmacSecret(paymentInfo.getSkinCode());
        final String hmacAlgorithm = adyenConfigProperties.getHmacAlgorithm(paymentInfo.getSkinCode());

        final String merchantSignature;
        if ("HmacSHA1".equals(hmacAlgorithm)) {
            merchantSignature = signer.signFormParameters(amount,
                                                          currency,
                                                          paymentInfo.getShipBeforeDate(),
                                                          paymentData.getPaymentTransactionExternalKey(),
                                                          paymentInfo.getSkinCode(),
                                                          merchantAccount,
                                                          userData.getShopperEmail(),
                                                          userData.getShopperReference(),
                                                          paymentInfo.getContract(),
                                                          paymentInfo.getAllowedMethods(),
                                                          paymentInfo.getSessionValidity(),
                                                          hmacSecret,
                                                          hmacAlgorithm);
        } else {
            merchantSignature = signer.signFormParameters(request,
                                                          hmacSecret,
                                                          hmacAlgorithm);
        }
        request.put("merchantSig", merchantSignature);

        try {
            setSplitSettlementData(merchantSignature, hmacSecret, hmacAlgorithm);
        } catch (final SignatureGenerationException e) {
            throw new RuntimeException(e);
        }

        return Maps.<String, String>filterValues(request, Predicates.<String>notNull());
    }

    private void setShopperData() {
        request.put("shopper.firstName", userData.getFirstName());
        request.put("shopper.infix", userData.getInfix());
        request.put("shopper.lastName", userData.getLastName());
        if (userData.getGender() != null) {
            request.put("shopper.gender", userData.getGender().toUpperCase());
        }
        request.put("shopper.telephoneNumber", userData.getTelephoneNumber());
        request.put("shopper.socialSecurityNumber", userData.getSocialSecurityNumber());
        if (userData.getDateOfBirth() != null) {
            request.put("shopper.dateOfBirthDayOfMonth", String.valueOf(userData.getDateOfBirth().getDayOfMonth()));
            request.put("shopper.dateOfBirthMonth", String.valueOf(userData.getDateOfBirth().getMonthOfYear()));
            request.put("shopper.dateOfBirthYear", String.valueOf(userData.getDateOfBirth().getYear()));
        }
        request.put("shopperEmail", userData.getShopperEmail());
        request.put("shopperIP", userData.getShopperIP());
        request.put("shopperReference", userData.getShopperReference());
    }

    private void setSplitSettlementData(final String merchantSignature, final String hmacSecret, final String hmacAlgorithm) throws SignatureGenerationException {
        if (splitSettlementData != null) {
            final Map<String, String> entries = new SplitSettlementParamsBuilder().createSignedParamsFrom(splitSettlementData,
                                                                                                          merchantSignature,
                                                                                                          signer,
                                                                                                          hmacSecret,
                                                                                                          hmacAlgorithm);
            request.putAll(entries);
        }
    }
}
