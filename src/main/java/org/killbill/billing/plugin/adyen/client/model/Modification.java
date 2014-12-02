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

import java.util.Currency;

import com.google.common.base.Preconditions;

public class Modification {

    private final Long billingId;
    private final Integer customerId;
    private final Integer dealId;
    private final String externalReference;
    private final Long amount;
    private final Integer appDomainId;
    private final PaymentInfo paymentInfo;

    public Modification(final Long billingId,
                        final Integer customerId,
                        final Integer dealId,
                        final String externalReference,
                        final Long amount,
                        final Integer appDomainId,
                        final PaymentInfo paymentInfo) {
        Preconditions.checkNotNull(billingId, "billingId");
        Preconditions.checkNotNull(customerId, "customerId");
        Preconditions.checkNotNull(dealId, "dealId");
        Preconditions.checkNotNull(externalReference, "externalReference");
        Preconditions.checkNotNull(amount, "amount");
        Preconditions.checkArgument(amount >= 0, "amount is negative");
        Preconditions.checkNotNull(paymentInfo.getPaymentProvider(), "paymentProvider");
        Preconditions.checkNotNull(appDomainId, "appDomainId");

        this.customerId = customerId;
        this.dealId = dealId;
        this.externalReference = externalReference;
        this.amount = amount;
        this.billingId = billingId;
        this.appDomainId = appDomainId;
        this.paymentInfo = paymentInfo;
    }

    public String getCountryIsoCode() {
        return paymentInfo.getPaymentProvider().getCountryIsoCode();
    }

    public Long getBillingId() {
        return billingId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public Integer getDealId() {
        return dealId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public Long getAmount() {
        return amount;
    }

    public PaymentProvider getPaymentProvider() {
        return paymentInfo.getPaymentProvider();
    }

    public Currency getCurrency() {
        return paymentInfo.getPaymentProvider().getCurrency();
    }

    public Integer getAppDomainId() {
        return appDomainId;
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Modification{");
        sb.append("billingId=").append(billingId);
        sb.append(", customerId=").append(customerId);
        sb.append(", dealId=").append(dealId);
        sb.append(", externalReference='").append(externalReference).append('\'');
        sb.append(", amount=").append(amount);
        sb.append(", appDomainId=").append(appDomainId);
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

        final Modification that = (Modification) o;

        if (amount != null ? !amount.equals(that.amount) : that.amount != null) {
            return false;
        }
        if (appDomainId != null ? !appDomainId.equals(that.appDomainId) : that.appDomainId != null) {
            return false;
        }
        if (billingId != null ? !billingId.equals(that.billingId) : that.billingId != null) {
            return false;
        }
        if (customerId != null ? !customerId.equals(that.customerId) : that.customerId != null) {
            return false;
        }
        if (dealId != null ? !dealId.equals(that.dealId) : that.dealId != null) {
            return false;
        }
        if (externalReference != null ? !externalReference.equals(that.externalReference) : that.externalReference != null) {
            return false;
        }
        if (paymentInfo != null ? !paymentInfo.equals(that.paymentInfo) : that.paymentInfo != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = billingId != null ? billingId.hashCode() : 0;
        result = 31 * result + (customerId != null ? customerId.hashCode() : 0);
        result = 31 * result + (dealId != null ? dealId.hashCode() : 0);
        result = 31 * result + (externalReference != null ? externalReference.hashCode() : 0);
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (appDomainId != null ? appDomainId.hashCode() : 0);
        result = 31 * result + (paymentInfo != null ? paymentInfo.hashCode() : 0);
        return result;
    }
}
