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

package org.killbill.billing.plugin.adyen.client.model;

import java.util.Map;

import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;

import com.google.common.base.Strings;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_FROM_HPP_TRANSACTION_STATUS;

public class HppCompletedResult {

    private final String pspReference;
    private final String authResult;
    private final PaymentServiceProviderResult pspResult;
    private final String merchantReference;
    private final String skinCode;
    private final String merchantSig;
    private final String paymentMethod;
    private final String shopperLocale;
    private final String merchantReturnData;
    private final Map<String, String> additionalData;

    public HppCompletedResult(final Map<String, String> requestParameterMap) {
        this(requestParameterMap.get("pspReference"),
             requestParameterMap.get("authResult"),
             getPspResult(requestParameterMap),
             requestParameterMap.get("merchantReference"),
             requestParameterMap.get("skinCode"),
             requestParameterMap.get("merchantSig"),
             requestParameterMap.get("paymentMethod"),
             requestParameterMap.get("shopperLocale"),
             requestParameterMap.get("merchantReturnData"),
             requestParameterMap);
    }

    public HppCompletedResult(final String pspReference,
                              final String authResult,
                              final PaymentServiceProviderResult pspResult,
                              final String merchantReference,
                              final String skinCode,
                              final String merchantSig,
                              final String paymentMethod,
                              final String shopperLocale,
                              final String merchantReturnData,
                              final Map<String, String> additionalData) {
        this.pspReference = Strings.isNullOrEmpty(pspReference) || pspReference.equals("\"\"") ? null : pspReference;
        this.authResult = authResult;
        this.pspResult = pspResult;
        this.merchantReference = merchantReference;
        this.skinCode = skinCode;
        this.merchantSig = merchantSig;
        this.paymentMethod = paymentMethod;
        this.shopperLocale = shopperLocale;
        this.merchantReturnData = merchantReturnData;
        this.additionalData = additionalData;
    }

    private static PaymentServiceProviderResult getPspResult(final Map<String, String> requestParameterMap) {
        if (requestParameterMap.get("authResult") != null) {
            // If there is an authResult from Adyen, transform it into PaymentServiceProviderResult
            return PaymentServiceProviderResult.getPaymentResultForId(requestParameterMap.get("authResult"));
        } else if (requestParameterMap.get("pspReference") != null && requestParameterMap.get(PROPERTY_FROM_HPP_TRANSACTION_STATUS) != null) {
            // The request came from Adyen
            return PaymentServiceProviderResult.getPaymentResultForPluginStatus(PaymentPluginStatus.valueOf(requestParameterMap.get(PROPERTY_FROM_HPP_TRANSACTION_STATUS)));
        } else {
            // By convention
            return PaymentServiceProviderResult.REDIRECT_SHOPPER;
        }
    }

    public String getPspReference() {
        return pspReference;
    }

    public String getAuthResult() {
        return authResult;
    }

    public PaymentServiceProviderResult getPspResult() {
        return pspResult;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public String getSkinCode() {
        return skinCode;
    }

    public String getMerchantSig() {
        return merchantSig;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getShopperLocale() {
        return shopperLocale;
    }

    public String getMerchantReturnData() {
        return merchantReturnData;
    }

    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HppCompletedResult{");
        sb.append("pspReference='").append(pspReference).append('\'');
        sb.append(", authResult='").append(authResult).append('\'');
        sb.append(", pspResult=").append(pspResult);
        sb.append(", merchantReference='").append(merchantReference).append('\'');
        sb.append(", skinCode='").append(skinCode).append('\'');
        sb.append(", merchantSig='").append(merchantSig).append('\'');
        sb.append(", paymentMethod='").append(paymentMethod).append('\'');
        sb.append(", shopperLocale='").append(shopperLocale).append('\'');
        sb.append(", merchantReturnData='").append(merchantReturnData).append('\'');
        sb.append(", additionalData=").append(additionalData);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HppCompletedResult that = (HppCompletedResult) o;

        if (pspReference != null ? !pspReference.equals(that.pspReference) : that.pspReference != null) {
            return false;
        }
        if (authResult != null ? !authResult.equals(that.authResult) : that.authResult != null) {
            return false;
        }
        if (pspResult != that.pspResult) {
            return false;
        }
        if (merchantReference != null ? !merchantReference.equals(that.merchantReference) : that.merchantReference != null) {
            return false;
        }
        if (skinCode != null ? !skinCode.equals(that.skinCode) : that.skinCode != null) {
            return false;
        }
        if (merchantSig != null ? !merchantSig.equals(that.merchantSig) : that.merchantSig != null) {
            return false;
        }
        if (paymentMethod != null ? !paymentMethod.equals(that.paymentMethod) : that.paymentMethod != null) {
            return false;
        }
        if (shopperLocale != null ? !shopperLocale.equals(that.shopperLocale) : that.shopperLocale != null) {
            return false;
        }
        if (merchantReturnData != null ? !merchantReturnData.equals(that.merchantReturnData) : that.merchantReturnData != null) {
            return false;
        }
        return additionalData != null ? additionalData.equals(that.additionalData) : that.additionalData == null;

    }

    @Override
    public int hashCode() {
        int result = pspReference != null ? pspReference.hashCode() : 0;
        result = 31 * result + (authResult != null ? authResult.hashCode() : 0);
        result = 31 * result + (pspResult != null ? pspResult.hashCode() : 0);
        result = 31 * result + (merchantReference != null ? merchantReference.hashCode() : 0);
        result = 31 * result + (skinCode != null ? skinCode.hashCode() : 0);
        result = 31 * result + (merchantSig != null ? merchantSig.hashCode() : 0);
        result = 31 * result + (paymentMethod != null ? paymentMethod.hashCode() : 0);
        result = 31 * result + (shopperLocale != null ? shopperLocale.hashCode() : 0);
        result = 31 * result + (merchantReturnData != null ? merchantReturnData.hashCode() : 0);
        result = 31 * result + (additionalData != null ? additionalData.hashCode() : 0);
        return result;
    }
}
