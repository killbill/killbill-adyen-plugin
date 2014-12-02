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

package org.killbill.billing.plugin.adyen.client.payment.builder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.killbill.adyen.payment.AnyType2AnyTypeMap;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.payment.service.PayPalCountryCodes;

public class HPPRequestBuilder extends RequestBuilder<Map<String, String>> {

    public HPPRequestBuilder() {
        super(new TreeMap<String, String>());
    }

    public HPPRequestBuilder withShopperLocale(final PaymentType paymentType, final Locale customerLocale) {
        final String shopperLocale;
        if (paymentType == PaymentType.PAYPAL
            && PayPalCountryCodes.isNotPayPalIsoCode(AdyenConfigProperties.gbToUK(customerLocale).getCountry())
            && PayPalCountryCodes.isNotPayPalLocale(customerLocale)) {
            shopperLocale = Locale.US.toString();
        } else {
            shopperLocale = AdyenConfigProperties.gbToUK(customerLocale).toString();
        }
        request.put("shopperLocale", shopperLocale);
        return this;
    }

    public HPPRequestBuilder withBrandCodeAndOrAllowedMethods(final PaymentInfo paymentInfo) {
        final String variant = paymentInfo.getPaymentProvider().getHppVariantOverride();
        if (variant != null) {
            request.put("brandCode", variant);
        } else {
            request.put("allowedMethods", paymentInfo.getPaymentProvider().getAllowedMethods());
            if (paymentInfo.getPaymentProvider().getPaymentType() != PaymentType.DEBITCARDS_HPP) {
                request.put("brandCode", paymentInfo.getPaymentProvider().getAllowedMethods());
            }
        }
        return this;
    }

    public HPPRequestBuilder withSkinCode(final String skinCode) {
        request.put("skinCode", skinCode);
        return this;
    }

    public HPPRequestBuilder withMerchantAccount(final String merchantAccount) {
        request.put("merchantAccount", merchantAccount);
        return this;
    }

    public HPPRequestBuilder withCountryCode(final String countryCode) {
        request.put("countryCode", countryCode);
        return this;
    }

    public HPPRequestBuilder withMerchantReference(final String value) {
        request.put("merchantReference", value);
        return this;
    }

    public HPPRequestBuilder withPaymentAmount(final Number value) {
        return withPaymentAmount(value.toString());
    }

    public HPPRequestBuilder withPaymentAmount(final String value) {
        request.put("paymentAmount", value);
        return this;
    }

    public HPPRequestBuilder withCurrencyCode(final String value) {
        request.put("currencyCode", value);
        return this;
    }

    public HPPRequestBuilder withShipBeforeDate(final String value) {
        request.put("shipBeforeDate", value);
        return this;
    }

    public HPPRequestBuilder withShopperEmail(final String value) {
        request.put("shopperEmail", value);
        return this;
    }

    public HPPRequestBuilder withShopperReference(final Number value) {
        return withShopperReference(value.toString());
    }

    public HPPRequestBuilder withShopperReference(final String value) {
        request.put("shopperReference", value);
        return this;
    }

    public HPPRequestBuilder withRecurringContract(final PaymentProvider paymentProvider) {
        if (paymentProvider.isRecurringEnabled()) {
            request.put("recurringContract", paymentProvider.getRecurringType().name());
        }
        return this;
    }

    public HPPRequestBuilder withSessionValidity(final String sessionValidity) {
        request.put("sessionValidity", sessionValidity);
        return this;
    }

    public HPPRequestBuilder withResURL(final String value) {
        request.put("resURL", value);
        return this;
    }

    public HPPRequestBuilder withMerchantSig(final String value) {
        request.put("merchantSig", value);
        return this;
    }

    public HPPRequestBuilder withSplitSettlementParameters(final Map<String, String> params) {
        request.putAll(params);
        return this;
    }

    @Override
    protected List<AnyType2AnyTypeMap.Entry> getAdditionalData() {
        throw new UnsupportedOperationException();
    }
}
