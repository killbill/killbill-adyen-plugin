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

import java.util.UUID;

public class PaymentData<I extends PaymentInfo> {

    private UUID paymentId;
    private String paymentInternalRef;
    private String paymentTxnInternalRef;
    private I paymentInfo;

    public String getPaymentInternalRef() {
        return paymentInternalRef;
    }

    public void setPaymentInternalRef(final String paymentInternalRef) {
        this.paymentInternalRef = paymentInternalRef;
    }

    public String getPaymentTxnInternalRef() {
        return paymentTxnInternalRef;
    }

    public void setPaymentTxnInternalRef(final String paymentTxnInternalRef) {
        this.paymentTxnInternalRef = paymentTxnInternalRef;
    }

    public I getPaymentInfo() {
        return paymentInfo;
    }

    public void setPaymentInfo(final I paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(final UUID paymentId) {
        this.paymentId = paymentId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentData{");
        sb.append("paymentId=").append(paymentId);
        sb.append(", paymentInternalRef='").append(paymentInternalRef).append('\'');
        sb.append(", paymentTxnInternalRef='").append(paymentTxnInternalRef).append('\'');
        sb.append(", paymentInfo=").append(paymentInfo);
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

        final PaymentData that = (PaymentData) o;

        if (paymentId != null ? !paymentId.equals(that.paymentId) : that.paymentId != null) {
            return false;
        }
        if (paymentInfo != null ? !paymentInfo.equals(that.paymentInfo) : that.paymentInfo != null) {
            return false;
        }
        if (paymentInternalRef != null ? !paymentInternalRef.equals(that.paymentInternalRef) : that.paymentInternalRef != null) {
            return false;
        }
        if (paymentTxnInternalRef != null ? !paymentTxnInternalRef.equals(that.paymentTxnInternalRef) : that.paymentTxnInternalRef != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = paymentId != null ? paymentId.hashCode() : 0;
        result = 31 * result + (paymentInternalRef != null ? paymentInternalRef.hashCode() : 0);
        result = 31 * result + (paymentTxnInternalRef != null ? paymentTxnInternalRef.hashCode() : 0);
        result = 31 * result + (paymentInfo != null ? paymentInfo.hashCode() : 0);
        return result;
    }
}
