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

package org.killbill.billing.plugin.adyen.client.model;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class PurchaseResult extends FrontendForm {

    private final PaymentServiceProviderResult result;
    private final String authCode;
    private final String pspReference;
    private final String reason;
    private final String resultCode;
    private final String reference;
    private final List<PaymentServiceProviderErrorCodes> errorCodes;
    private final String paymentInternalRef;

    public PurchaseResult(final PaymentServiceProviderResult result,
                          final String reason,
                          final String reference,
                          final List<PaymentServiceProviderErrorCodes> errorCodes,
                          final String paymentInternalRef) {
        this(result, null, null, reason, null, reference, errorCodes, paymentInternalRef, null, null);
    }

    public PurchaseResult(final PaymentServiceProviderResult result,
                          final String authCode,
                          final String pspReference,
                          final String reason,
                          final String resultCode,
                          final String paymentInternalRef,
                          final String formUrl,
                          final Map<String, String> formParameter) {
        this(result, authCode, pspReference, reason, resultCode, null, null, paymentInternalRef, formUrl, formParameter);
    }

    public PurchaseResult(final PaymentServiceProviderResult result,
                          final String authCode,
                          final String pspReference,
                          final String reason,
                          final String resultCode,
                          final String paymentInternalRef) {
        this(result, authCode, pspReference, reason, resultCode, null, null, paymentInternalRef, null, null);
    }

    private PurchaseResult(@Nullable final PaymentServiceProviderResult result,
                           final String authCode,
                           final String pspReference,
                           final String reason,
                           @Nullable final String resultCode,
                           final String reference,
                           @Nullable final List<PaymentServiceProviderErrorCodes> errorCodes,
                           final String paymentInternalRef,
                           final String formUrl,
                           final Map<String, String> formParameter) {
        super(Objects.firstNonNull(formParameter, ImmutableMap.<String, String>of()), formUrl);

        this.result = Objects.firstNonNull(result, PaymentServiceProviderResult.REDIRECT_SHOPPER);
        this.authCode = authCode;
        this.pspReference = pspReference;
        this.reason = reason;
        this.resultCode = Objects.firstNonNull(resultCode, this.result.getId());
        this.reference = reference;
        this.errorCodes = Objects.firstNonNull(errorCodes, ImmutableList.<PaymentServiceProviderErrorCodes>of());
        this.paymentInternalRef = paymentInternalRef;
    }

    public String getReference() {
        return reference;
    }

    public String getPaymentInternalRef() {
        return paymentInternalRef;
    }

    public String getAuthCode() {
        return authCode;
    }

    public String getReason() {
        return reason;
    }

    public PaymentServiceProviderResult getResult() {
        return result;
    }

    public String getPspReference() {
        return pspReference;
    }

    public String getResultCode() {
        return resultCode;
    }

    public List<PaymentServiceProviderErrorCodes> getErrorCodes() {
        return errorCodes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PurchaseResult{");
        sb.append("result=").append(result);
        sb.append(", authCode='").append(authCode).append('\'');
        sb.append(", pspReference='").append(pspReference).append('\'');
        sb.append(", reason='").append(reason).append('\'');
        sb.append(", resultCode='").append(resultCode).append('\'');
        sb.append(", reference='").append(reference).append('\'');
        sb.append(", errorCodes=").append(errorCodes);
        sb.append(", paymentInternalRef='").append(paymentInternalRef).append('\'');
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

        final PurchaseResult that = (PurchaseResult) o;

        if (authCode != null ? !authCode.equals(that.authCode) : that.authCode != null) {
            return false;
        }
        if (errorCodes != null ? !errorCodes.equals(that.errorCodes) : that.errorCodes != null) {
            return false;
        }
        if (paymentInternalRef != null ? !paymentInternalRef.equals(that.paymentInternalRef) : that.paymentInternalRef != null) {
            return false;
        }
        if (pspReference != null ? !pspReference.equals(that.pspReference) : that.pspReference != null) {
            return false;
        }
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) {
            return false;
        }
        if (reference != null ? !reference.equals(that.reference) : that.reference != null) {
            return false;
        }
        if (result != that.result) {
            return false;
        }
        if (resultCode != null ? !resultCode.equals(that.resultCode) : that.resultCode != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = result != null ? result.hashCode() : 0;
        result1 = 31 * result1 + (authCode != null ? authCode.hashCode() : 0);
        result1 = 31 * result1 + (pspReference != null ? pspReference.hashCode() : 0);
        result1 = 31 * result1 + (reason != null ? reason.hashCode() : 0);
        result1 = 31 * result1 + (resultCode != null ? resultCode.hashCode() : 0);
        result1 = 31 * result1 + (reference != null ? reference.hashCode() : 0);
        result1 = 31 * result1 + (errorCodes != null ? errorCodes.hashCode() : 0);
        result1 = 31 * result1 + (paymentInternalRef != null ? paymentInternalRef.hashCode() : 0);
        return result1;
    }
}
