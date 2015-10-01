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

import com.google.common.base.Optional;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus;

import java.util.HashMap;
import java.util.Map;

public class PaymentModificationResponse {

    private Map<Object, Object> additionalData;
    private String pspReference;
    private String response;
    private Optional<AdyenCallErrorStatus> adyenCallErrorStatus = Optional.absent();

    public PaymentModificationResponse(final String response, final String pspReference, final Map<Object, Object> additionalData) {
        this.additionalData = additionalData;
        this.pspReference = pspReference;
        this.response = response;
    }

    public PaymentModificationResponse(final String pspReference, final AdyenCallErrorStatus adyenCallErrorStatus) {
        this.pspReference = pspReference;
        this.adyenCallErrorStatus = Optional.of(adyenCallErrorStatus);
    }

    public PaymentModificationResponse(final String response, final String pspReference) {
        this(response, pspReference, new HashMap<Object, Object>());
    }

    public boolean isSuccess() {
        return !adyenCallErrorStatus.isPresent();
    }


    public Map<Object, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(final Map<Object, Object> additionalData) {
        this.additionalData = additionalData;
    }

    public String getPspReference() {
        return pspReference;
    }

    public void setPspReference(final String pspReference) {
        this.pspReference = pspReference;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(final String response) {
        this.response = response;
    }

    public Optional<AdyenCallErrorStatus> getAdyenCallErrorStatus() {
        return adyenCallErrorStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentModificationResponse that = (PaymentModificationResponse) o;

        if (additionalData != null ? !additionalData.equals(that.additionalData) : that.additionalData != null)
            return false;
        if (pspReference != null ? !pspReference.equals(that.pspReference) : that.pspReference != null) return false;
        //noinspection SimplifiableIfStatement
        if (response != null ? !response.equals(that.response) : that.response != null) return false;
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

    @Override
    public String toString() {
        return "PaymentModificationResponse{" +
                "additionalData=" + additionalData +
                ", pspReference='" + pspReference + '\'' +
                ", response='" + response + '\'' +
                ", adyenCallErrorStatus=" + adyenCallErrorStatus +
                '}';
    }
}
