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

import static com.google.common.base.Preconditions.checkArgument;

public abstract class PaymentInfo {

    private String recurringDetailId = null;

    private boolean continousAuthenticationEnabled = false;

    private PaymentProvider paymentProvider;

    protected PaymentInfo(final PaymentProvider paymentProvider) {
        checkArgument(paymentProvider != null, "paymentProvider is null");
        this.paymentProvider = paymentProvider;
    }

    public PaymentProvider getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(final PaymentProvider paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    /**
     * Returns if the continuous authentication interaction with Adyen should be used
     *
     * @return true if the continuous authentication interaction with Adyen should be used, otherwise false
     */
    public boolean isContinousAuthenticationEnabled() {
        return continousAuthenticationEnabled;
    }

    public String getRecurringDetailId() {
        return recurringDetailId;
    }

    public void setRecurringDetailId(final String recurringDetailId) {
        this.recurringDetailId = recurringDetailId;
    }
}
