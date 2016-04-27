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

public class PaymentData<I extends PaymentInfo> {

    private final String paymentTransactionExternalKey;
    private final I paymentInfo;

    public PaymentData(final String paymentTransactionExternalKey, final I paymentInfo) {
        this.paymentTransactionExternalKey = paymentTransactionExternalKey;
        this.paymentInfo = paymentInfo;
    }

    public String getPaymentTransactionExternalKey() {
        return paymentTransactionExternalKey;
    }

    public I getPaymentInfo() {
        return paymentInfo;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PaymentData{");
        sb.append("paymentTransactionExternalKey='").append(paymentTransactionExternalKey).append('\'');
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

        final PaymentData<?> that = (PaymentData<?>) o;

        if (paymentTransactionExternalKey != null ? !paymentTransactionExternalKey.equals(that.paymentTransactionExternalKey) : that.paymentTransactionExternalKey != null) {
            return false;
        }
        return paymentInfo != null ? paymentInfo.equals(that.paymentInfo) : that.paymentInfo == null;

    }

    @Override
    public int hashCode() {
        int result = paymentTransactionExternalKey != null ? paymentTransactionExternalKey.hashCode() : 0;
        result = 31 * result + (paymentInfo != null ? paymentInfo.hashCode() : 0);
        return result;
    }
}
