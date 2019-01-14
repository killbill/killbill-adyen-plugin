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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.HppCompletedResult;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.killbill.billing.plugin.util.KillBillMoney.toMinorUnits;

public class AdyenPaymentServiceProviderHostedPaymentPagePort extends BaseAdyenPaymentServiceProviderPort implements Closeable {

    private final AdyenConfigProperties adyenConfigProperties;
    private final AdyenRequestFactory adyenRequestFactory;
    private final DirectoryClient directoryClient;

    public AdyenPaymentServiceProviderHostedPaymentPagePort(final AdyenConfigProperties adyenConfigProperties,
                                                            final AdyenRequestFactory adyenRequestFactory,
                                                            @Nullable final DirectoryClient directoryClient) {
        this.adyenConfigProperties = adyenConfigProperties;
        this.adyenRequestFactory = adyenRequestFactory;
        this.directoryClient = directoryClient;

        this.logger = LoggerFactory.getLogger(AdyenPaymentServiceProviderHostedPaymentPagePort.class);
    }

    @Override
    public void close() throws IOException {
        if (directoryClient != null) {
            directoryClient.close();
        }
    }

    public Map<String, String> getFormParameter(final String merchantAccount, final PaymentData paymentData, final UserData userData, final SplitSettlementData splitSettlementData) throws SignatureGenerationException {
        logTransaction("createHppRequest", userData, merchantAccount, paymentData, null, null);
        return adyenRequestFactory.createHppRequest(merchantAccount, paymentData, userData, splitSettlementData);
    }

    public Map getDirectory(final String merchantAccount,
                            final BigDecimal amount,
                            final Currency currency,
                            final String merchantReference,
                            final String skinCode,
                            final String sessionValidity,
                            @Nullable final String countryIsoCode) {
        if (directoryClient == null) {
            return null;
        }

        final Map<String, String> params = new TreeMap<String, String>();
        // Mandatory
        params.put("merchantAccount", merchantAccount);
        params.put("currencyCode", currency.toString());
        params.put("paymentAmount", String.valueOf(toMinorUnits(currency.toString(), amount)));
        params.put("merchantReference", merchantReference);
        params.put("skinCode", skinCode);
        params.put("sessionValidity", sessionValidity);
        // Optional
        if (countryIsoCode != null) {
            params.put("countryCode", countryIsoCode);
        }

        final Signer signer = new Signer();
        final String hmacSecret = adyenConfigProperties.getHmacSecret(skinCode);
        final String hmacAlgorithm = adyenConfigProperties.getHmacAlgorithm(skinCode);
        params.put("merchantSig", signer.signFormParameters(params, hmacSecret, hmacAlgorithm));

        return directoryClient.getDirectory(params);
    }

    @SuppressWarnings("deprecation")
    public HppCompletedResult parseAndVerifyRequestIntegrity(final Map<String, String> requestParameterMap) {
        final HppCompletedResult hppCompletedResult = new HppCompletedResult(requestParameterMap);
        final String merchantSig = requestParameterMap.get("merchantSig");
        // Note! It's the caller responsibility to verify a merchantSig is passed to enable request tampering verification
        if (merchantSig == null) {
            return hppCompletedResult;
        }

        final String hmacSecret = adyenConfigProperties.getHmacSecret(hppCompletedResult.getSkinCode());
        final String hmacAlgorithm = adyenConfigProperties.getHmacAlgorithm(hppCompletedResult.getSkinCode());
        final Signer signer = new Signer();
        final String expectedMerchantSignature;
        if ("HmacSHA1".equals(hmacAlgorithm)) {
            expectedMerchantSignature = signer.signFormParameters(hppCompletedResult.getAuthResult(),
                                                                  hppCompletedResult.getPspReference(),
                                                                  hppCompletedResult.getMerchantReference(),
                                                                  hppCompletedResult.getSkinCode(),
                                                                  hppCompletedResult.getMerchantReturnData(),
                                                                  hmacSecret,
                                                                  hmacAlgorithm);
        } else {
            final Map<String, String> params = new HashMap<String, String>();
            params.put("pspReference", hppCompletedResult.getPspReference());
            params.put("authResult", hppCompletedResult.getAuthResult());
            params.put("merchantReference", hppCompletedResult.getMerchantReference());
            params.put("skinCode", hppCompletedResult.getSkinCode());
            params.put("paymentMethod", hppCompletedResult.getPaymentMethod());
            params.put("shopperLocale", hppCompletedResult.getShopperLocale());
            params.put("merchantReturnData", hppCompletedResult.getMerchantReturnData());
            expectedMerchantSignature = signer.signFormParameters(params,
                                                                  hmacSecret,
                                                                  hmacAlgorithm);
        }

        if (merchantSig.equals(expectedMerchantSignature)) {
            return hppCompletedResult;
        } else {
            logger.warn("Signature mismatch: expectedMerchantSignature='{}', requestParameterMap='{}'", expectedMerchantSignature, requestParameterMap);
            return new HppCompletedResult(hppCompletedResult.getPspReference(),
                                          hppCompletedResult.getAuthResult(),
                                          PaymentServiceProviderResult.CANCELLED,
                                          hppCompletedResult.getMerchantReference(),
                                          hppCompletedResult.getSkinCode(),
                                          hppCompletedResult.getMerchantSig(),
                                          hppCompletedResult.getPaymentMethod(),
                                          hppCompletedResult.getShopperLocale(),
                                          hppCompletedResult.getMerchantReturnData(),
                                          requestParameterMap);
        }
    }
}
