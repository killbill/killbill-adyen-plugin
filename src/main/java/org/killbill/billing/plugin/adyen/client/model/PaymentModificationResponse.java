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

import java.util.HashMap;
import java.util.Map;

public class PaymentModificationResponse {

    private Map<Object, Object> additionalData;
    private String pspReference;
    private String response;

    public PaymentModificationResponse(final String response, final String pspReference, final Map<Object, Object> additionalData) {
        this.additionalData = additionalData;
        this.pspReference = pspReference;
        this.response = response;
    }

    public PaymentModificationResponse(final String response, final String pspReference) {
        this(response, pspReference, new HashMap<Object, Object>());
    }

    public PaymentModificationResponse() {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentModificationResponse{");
        sb.append("additionalData=").append(additionalData);
        sb.append(", pspReference='").append(pspReference).append('\'');
        sb.append(", response='").append(response).append('\'');
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

        final PaymentModificationResponse that = (PaymentModificationResponse) o;

        if (additionalData != null ? !additionalData.equals(that.additionalData) : that.additionalData != null) {
            return false;
        }
        if (pspReference != null ? !pspReference.equals(that.pspReference) : that.pspReference != null) {
            return false;
        }
        if (response != null ? !response.equals(that.response) : that.response != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = additionalData != null ? additionalData.hashCode() : 0;
        result = 31 * result + (pspReference != null ? pspReference.hashCode() : 0);
        result = 31 * result + (response != null ? response.hashCode() : 0);
        return result;
    }
}
