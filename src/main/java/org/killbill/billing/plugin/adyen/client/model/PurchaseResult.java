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

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class PurchaseResult extends FrontendForm {

    private final Optional<PaymentServiceProviderResult> result;
    private final String authCode;
    private final String pspReference;
    private final String reason;
    private final String resultCode;
    private final String reference;
    private final List<PaymentServiceProviderErrorCodes> errorCodes;
    private final String paymentInternalRef;
    private final AdyenCallErrorStatus adyenCallErrorStatus;

    public PurchaseResult(final String paymentInternalRef,
                          final AdyenCallErrorStatus adyenCallErrorStatus) {
        this(Optional.<PaymentServiceProviderResult>absent(), null, null, null, null, null, ImmutableList.<PaymentServiceProviderErrorCodes>of(), paymentInternalRef, null, null, adyenCallErrorStatus);
    }

    public PurchaseResult(final PaymentServiceProviderResult result,
                          final String authCode,
                          final String pspReference,
                          final String reason,
                          final String resultCode,
                          final String paymentInternalRef,
                          final String formUrl,
                          final Map<String, String> formParameter) {
        this(Optional.of(result), authCode, pspReference, reason, resultCode, null, null, paymentInternalRef, formUrl, formParameter, null);
    }

    public PurchaseResult(final PaymentServiceProviderResult result,
                          final String authCode,
                          final String pspReference,
                          final String reason,
                          final String resultCode,
                          final String paymentInternalRef) {
        this(Optional.of(result), authCode, pspReference, reason, resultCode, null, null, paymentInternalRef, null, null, null);
    }

    private PurchaseResult(final Optional<PaymentServiceProviderResult> result,
                           final String authCode,
                           final String pspReference,
                           final String reason,
                           @Nullable final String resultCode,
                           final String reference,
                           @Nullable final List<PaymentServiceProviderErrorCodes> errorCodes,
                           final String paymentInternalRef,
                           final String formUrl,
                           final Map<String, String> formParameter,
                           AdyenCallErrorStatus adyenCallErrorStatus) {
        super(MoreObjects.firstNonNull(formParameter, ImmutableMap.<String, String>of()), formUrl);

        this.adyenCallErrorStatus = adyenCallErrorStatus;
        this.result = result;
        this.authCode = authCode;
        this.pspReference = pspReference;
        this.reason = reason;
        this.resultCode = resultCode;
        this.reference = reference;
        this.errorCodes = MoreObjects.firstNonNull(errorCodes, ImmutableList.<PaymentServiceProviderErrorCodes>of());
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

    public Optional<PaymentServiceProviderResult> getResult() {
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

    public Optional<AdyenCallErrorStatus> getAdyenCallErrorStatus() {
        return Optional.fromNullable(adyenCallErrorStatus);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PurchaseResult that = (PurchaseResult) o;

        if (result != null ? !result.equals(that.result) : that.result != null) return false;
        if (authCode != null ? !authCode.equals(that.authCode) : that.authCode != null) return false;
        if (pspReference != null ? !pspReference.equals(that.pspReference) : that.pspReference != null) return false;
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) return false;
        if (resultCode != null ? !resultCode.equals(that.resultCode) : that.resultCode != null) return false;
        if (reference != null ? !reference.equals(that.reference) : that.reference != null) return false;
        if (errorCodes != null ? !errorCodes.equals(that.errorCodes) : that.errorCodes != null) return false;
        //noinspection SimplifiableIfStatement
        if (paymentInternalRef != null ? !paymentInternalRef.equals(that.paymentInternalRef) : that.paymentInternalRef != null)
            return false;
        return adyenCallErrorStatus == that.adyenCallErrorStatus;

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
        result1 = 31 * result1 + (adyenCallErrorStatus != null ? adyenCallErrorStatus.hashCode() : 0);
        return result1;
    }

    @Override
    public String toString() {
        return "PurchaseResult{" +
                "result=" + result +
                ", authCode='" + authCode + '\'' +
                ", pspReference='" + pspReference + '\'' +
                ", reason='" + reason + '\'' +
                ", resultCode='" + resultCode + '\'' +
                ", reference='" + reference + '\'' +
                ", errorCodes=" + errorCodes +
                ", paymentInternalRef='" + paymentInternalRef + '\'' +
                ", adyenResponseStatus=" + adyenCallErrorStatus +
                '}';
    }
}
