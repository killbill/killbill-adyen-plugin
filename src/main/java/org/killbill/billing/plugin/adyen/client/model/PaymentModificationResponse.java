/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.adyen.payment.ModificationResult;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallResult;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import static org.killbill.billing.plugin.adyen.client.model.PurchaseResult.ADYEN_CALL_ERROR_STATUS;
import static org.killbill.billing.plugin.adyen.client.model.PurchaseResult.EXCEPTION_CLASS;
import static org.killbill.billing.plugin.adyen.client.model.PurchaseResult.EXCEPTION_MESSAGE;
import static org.killbill.billing.plugin.adyen.client.model.PurchaseResult.UNKNOWN;

public class PaymentModificationResponse {

    private final AdyenCallErrorStatus adyenCallErrorStatus;
    private final Map<Object, Object> additionalData;
    private final String pspReference;
    private final String response;

    public PaymentModificationResponse(final String response, final String pspReference, final Map<Object, Object> additionalData) {
        this(pspReference, response, null, additionalData);
    }

    public PaymentModificationResponse(final String pspReference, final AdyenCallResult<ModificationResult> adyenCallResult, final Map<Object, Object> additionalData) {
        this(pspReference, null, adyenCallResult.getResponseStatus().orNull(), additionalData);
    }

    private PaymentModificationResponse(final String pspReference,
                                        final String response,
                                        @Nullable final AdyenCallErrorStatus adyenCallErrorStatus,
                                        final Map<Object, Object> additionalData) {
        this.pspReference = pspReference;
        this.response = response;
        this.adyenCallErrorStatus = adyenCallErrorStatus;
        this.additionalData = additionalData;
    }

    /**
     * True if we received a well formed soap response from adyen.
     */
    public boolean isTechnicallySuccessful() {
        return !getAdyenCallErrorStatus().isPresent();
    }

    public Map<Object, Object> getAdditionalData() {
        return additionalData;
    }

    public String getPspReference() {
        return pspReference;
    }

    public String getResponse() {
        return response;
    }

    public Optional<AdyenCallErrorStatus> getAdyenCallErrorStatus() {
        return Optional.fromNullable(adyenCallErrorStatus);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentModificationResponse{");
        sb.append("adyenCallErrorStatus=").append(adyenCallErrorStatus);
        sb.append(", pspReference='").append(pspReference).append('\'');
        sb.append(", response='").append(response).append('\'');
        sb.append(", additionalData={");
        // Make sure to escape values, as they may contain spaces
        final Iterator<Object> iterator = additionalData.keySet().iterator();
        if (iterator.hasNext()) {
            final Object key = iterator.next();
            sb.append(key).append("='").append(additionalData.get(key)).append("'");
        }
        while (iterator.hasNext()) {
            final Object key = iterator.next();
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

        final PaymentModificationResponse that = (PaymentModificationResponse) o;

        if (additionalData != null ? !additionalData.equals(that.additionalData) : that.additionalData != null) {
            return false;
        }
        if (pspReference != null ? !pspReference.equals(that.pspReference) : that.pspReference != null) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if (response != null ? !response.equals(that.response) : that.response != null) {
            return false;
        }
        return !(adyenCallErrorStatus != null ? !adyenCallErrorStatus.equals(that.adyenCallErrorStatus) : that.adyenCallErrorStatus != null);

    }

    @Override
    public int hashCode() {
        int result = additionalData != null ? additionalData.hashCode() : 0;
        result = 31 * result + (pspReference != null ? pspReference.hashCode() : 0);
        result = 31 * result + (response != null ? response.hashCode() : 0);
        result = 31 * result + (adyenCallErrorStatus != null ? adyenCallErrorStatus.hashCode() : 0);
        return result;
    }
}
