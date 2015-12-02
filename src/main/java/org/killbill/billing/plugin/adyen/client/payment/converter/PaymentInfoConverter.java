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

package org.killbill.billing.plugin.adyen.client.payment.converter;

import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;

public interface PaymentInfoConverter<T extends PaymentInfo> {

    /**
     * @return {@code true} if this converter is handling the payment type
     */
    boolean supportsPaymentType(PaymentType type);

    /**
     * Convert a PaymentInfo Object into an object understandable by the payment service provider
     */
    Object convertPaymentInfoToPSPTransferObject(String holderName, T paymentInfo);

    /**
     * Return the matching BrowserInfo element for 3-D Secure authorisation (needed for maestro, optional for MasterCard/Visa)
     *
     * @return the BrowserInfo object or null if not needed
     */
    Object convertPaymentInfoFor3DSecureAuth(Long billedAmount, Card card);
}
