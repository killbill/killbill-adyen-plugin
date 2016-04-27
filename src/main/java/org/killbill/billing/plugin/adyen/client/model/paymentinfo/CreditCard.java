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

package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;

public class CreditCard extends Card {

    private Integer installments;
    private String variant;

    public CreditCard(final PaymentProvider paymentProvider) {
        super(paymentProvider);
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(final Integer installments) {
        this.installments = installments;
    }

    public String getValidCCSecCodeSizes() {
        return "3";
    }

    public void setVariant(final String variant) {
        this.variant = variant;
    }

    public String getVariant() {
        return variant;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CreditCard{");
        sb.append("installments=").append(installments);
        sb.append(", variant='").append(variant).append('\'');
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

        final CreditCard that = (CreditCard) o;

        if (installments != null ? !installments.equals(that.installments) : that.installments != null) {
            return false;
        }
        if (variant != null ? !variant.equals(that.variant) : that.variant != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = installments != null ? installments.hashCode() : 0;
        result = 31 * result + (variant != null ? variant.hashCode() : 0);
        return result;
    }
}
