/*
 * Copyright 2015-2016 Groupon, Inc
 * Copyright 2015-2016 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.payment.service;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.slf4j.Logger;

public abstract class BaseAdyenPaymentServiceProviderPort {

    protected void logOperation(final Logger logger, final String operation, @Nullable final PaymentData paymentData, @Nullable final UserData userData, @Nullable final String pspReference) {
        final StringBuilder stringBuilder = new StringBuilder("op='").append(operation).append("'");
        if (paymentData != null && paymentData.getAmount() != null) {
            stringBuilder.append(", amount='")
                         .append(paymentData.getAmount())
                         .append("'");
        }
        if (paymentData != null && paymentData.getPaymentTransactionExternalKey() != null) {
            stringBuilder.append(", paymentTransactionExternalKey='")
                         .append(paymentData.getPaymentTransactionExternalKey())
                         .append("'");
        }
        if (userData != null && userData.getShopperReference() != null) {
            stringBuilder.append(", customerId='")
                         .append(userData.getShopperReference())
                         .append("'");
        }
        if (pspReference != null) {
            stringBuilder.append(", pspReference='")
                         .append(pspReference)
                         .append("'");
        }
        logger.info(stringBuilder.toString());
    }
}
