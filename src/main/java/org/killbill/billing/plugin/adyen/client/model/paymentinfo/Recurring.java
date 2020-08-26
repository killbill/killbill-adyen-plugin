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

package org.killbill.billing.plugin.adyen.client.model.paymentinfo;

import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;

public class Recurring extends PaymentInfo {

    private String recurringDetailReference;
    private String cvc;

    public String getRecurringDetailReference() {
        return recurringDetailReference;
    }

    public void setRecurringDetailReference(final String recurringDetailReference) {
        this.recurringDetailReference = recurringDetailReference;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(final String cvc) {
        this.cvc = cvc;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Recurring{");
        sb.append("recurringDetailReference='").append(recurringDetailReference).append('\'');
        sb.append(", cvc='").append(cvc).append('\'');
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
        if (!super.equals(o)) {
            return false;
        }

        final Recurring recurring = (Recurring) o;

        if (recurringDetailReference != null ? !recurringDetailReference.equals(recurring.recurringDetailReference) : recurring.recurringDetailReference != null) {
            return false;
        }
        return cvc != null ? cvc.equals(recurring.cvc) : recurring.cvc == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (recurringDetailReference != null ? recurringDetailReference.hashCode() : 0);
        result = 31 * result + (cvc != null ? cvc.hashCode() : 0);
        return result;
    }
}
