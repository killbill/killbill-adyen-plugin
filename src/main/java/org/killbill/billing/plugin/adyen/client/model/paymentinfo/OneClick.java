/*
 * Copyright 2015 Groupon, Inc
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

public class OneClick extends Recurring {

    private String ccSecCode;

    public OneClick(final PaymentProvider paymentProvider) {
        super(paymentProvider);
        setContinuousAuthenticationEnabled(false);
    }

    public String getCcSecCode() {
        return ccSecCode;
    }

    public void setCcSecCode(final String ccSecCode) {
        this.ccSecCode = ccSecCode;
    }

    @Override
    public String toString() {
        return String.format("OneClick{recurringDetailId='%s', ccSecCode='***', installments=%d}",
                getRecurringDetailId(),
                getInstallments());
    }

}
