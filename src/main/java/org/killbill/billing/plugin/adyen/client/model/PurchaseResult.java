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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.adyen.payment.PaymentResult;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallResult;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.MoreObjects.firstNonNull;

public class PurchaseResult extends FrontendForm {

    public static final String ADYEN_CALL_ERROR_STATUS = "adyenCallErrorStatus";
    public static final String EXCEPTION_CLASS = "exceptionClass";
    public static final String EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String UNKNOWN = "";

    private final Optional<PaymentServiceProviderResult> result;
    private final String authCode;
    private final String pspReference;
    private final String reason;
    private final String resultCode;
    private final String reference;
    private final String paymentTransactionExternalKey;
    private final AdyenCallErrorStatus adyenCallErrorStatus;

    private Map<String, String> additionalData;

    public PurchaseResult(final String paymentTransactionExternalKey,
                          final AdyenCallResult<PaymentResult> adyenCallResult) {
        this(Optional.<PaymentServiceProviderResult>absent(),
             null,
             null,
             null,
             null,
             null,
             paymentTransactionExternalKey,
             null,
             null,
             adyenCallResult.getResponseStatus().isPresent() ? adyenCallResult.getResponseStatus().get() : null,
             ImmutableMap.<String, String>of(ADYEN_CALL_ERROR_STATUS, adyenCallResult.getResponseStatus().isPresent() ? adyenCallResult.getResponseStatus().get().name() : UNKNOWN,
                                             EXCEPTION_CLASS, adyenCallResult.getExceptionClass().isPresent() ? adyenCallResult.getExceptionClass().get() : UNKNOWN,
                                             EXCEPTION_MESSAGE, adyenCallResult.getExceptionMessage().isPresent() ? adyenCallResult.getExceptionMessage().get() : UNKNOWN));
    }

    public PurchaseResult(final HppCompletedResult hppCompletedResult) {
        this(hppCompletedResult.getPspResult(),
             null,
             hppCompletedResult.getPspReference(),
             null,
             null,
             hppCompletedResult.getMerchantReference(),
             hppCompletedResult.getAdditionalData());
    }

    public void setAdditionalData(final Map<String, String> additionalData) {
        this.additionalData = additionalData;
    }

    public PurchaseResult(final PaymentServiceProviderResult result,
                          final String authCode,
                          final String pspReference,
                          final String reason,
                          final String resultCode,
                          final String paymentTransactionExternalKey,
                          final String formUrl,
                          final Map<String, String> formParameter,
                          final Map<String, String> additionalData) {
        this(Optional.of(result), authCode, pspReference, reason, resultCode, null, paymentTransactionExternalKey, formUrl, formParameter, null, additionalData);
    }

    public PurchaseResult(final PaymentServiceProviderResult result,
                          final String authCode,
                          final String pspReference,
                          final String reason,
                          final String resultCode,
                          final String paymentTransactionExternalKey,
                          final Map<String, String> additionalData) {
        this(Optional.of(result), authCode, pspReference, reason, resultCode, null, paymentTransactionExternalKey, null, null, null, additionalData);
    }

    private PurchaseResult(final Optional<PaymentServiceProviderResult> result,
                           final String authCode,
                           final String pspReference,
                           final String reason,
                           @Nullable final String resultCode,
                           final String reference,
                           final String paymentTransactionExternalKey,
                           final String formUrl,
                           final Map<String, String> formParameter,
                           @Nullable final AdyenCallErrorStatus adyenCallErrorStatus,
                           final Map<String, String> additionalData) {
        super(firstNonNull(formParameter, ImmutableMap.<String, String>of()), formUrl);

        this.adyenCallErrorStatus = adyenCallErrorStatus;
        this.result = result;
        this.authCode = authCode;
        this.pspReference = pspReference;
        this.reason = reason;
        this.resultCode = resultCode;
        this.reference = reference;
        this.paymentTransactionExternalKey = paymentTransactionExternalKey;
        this.additionalData = firstNonNull(additionalData, ImmutableMap.<String, String>of());
    }

    public String getReference() {
        return reference;
    }

    public String getPaymentTransactionExternalKey() {
        return paymentTransactionExternalKey;
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

    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    /**
     * True if we received a well formed soap response from adyen.
     */
    @SuppressWarnings("unused")
    public boolean isTechnicallySuccessful() {
        return !getAdyenCallErrorStatus().isPresent();
    }

    public Optional<AdyenCallErrorStatus> getAdyenCallErrorStatus() {
        return Optional.fromNullable(adyenCallErrorStatus);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PurchaseResult{");
        sb.append("result='").append(result.isPresent() ? result.get() : null).append('\'');
        sb.append(", authCode='").append(authCode).append('\'');
        sb.append(", pspReference='").append(pspReference).append('\'');
        sb.append(", reason='").append(reason).append('\'');
        sb.append(", resultCode='").append(resultCode).append('\'');
        sb.append(", reference='").append(reference).append('\'');
        sb.append(", paymentTransactionExternalKey='").append(paymentTransactionExternalKey).append('\'');
        sb.append(", adyenCallErrorStatus=").append(adyenCallErrorStatus);
        sb.append(", additionalData={");
        // Make sure to escape values, as they may contain spaces (e.g. avsResult='4 AVS not supported for this card type')
        final Iterator<String> iterator = additionalData.keySet().iterator();
        if (iterator.hasNext()) {
            final String key = iterator.next();
            sb.append(key).append("='").append(additionalData.get(key)).append("'");
        }
        while (iterator.hasNext()) {
            final String key = iterator.next();
            sb.append(", ")
              .append(key)
              .append("='")
              .append(additionalData.get(key))
              .append("'");
        }
        sb.append("}}");
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

        if (result != null ? !result.equals(that.result) : that.result != null) {
            return false;
        }
        if (authCode != null ? !authCode.equals(that.authCode) : that.authCode != null) {
            return false;
        }
        if (pspReference != null ? !pspReference.equals(that.pspReference) : that.pspReference != null) {
            return false;
        }
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) {
            return false;
        }
        if (resultCode != null ? !resultCode.equals(that.resultCode) : that.resultCode != null) {
            return false;
        }
        if (reference != null ? !reference.equals(that.reference) : that.reference != null) {
            return false;
        }
        if (adyenCallErrorStatus != null ? !adyenCallErrorStatus.equals(that.adyenCallErrorStatus) : that.adyenCallErrorStatus != null) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if (additionalData != null ? !additionalData.equals(that.additionalData) : that.additionalData != null) {
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
        result1 = 31 * result1 + (paymentTransactionExternalKey != null ? paymentTransactionExternalKey.hashCode() : 0);
        result1 = 31 * result1 + (adyenCallErrorStatus != null ? adyenCallErrorStatus.hashCode() : 0);
        result1 = 31 * result1 + (additionalData != null ? additionalData.hashCode() : 0);
        return result1;
    }
}
