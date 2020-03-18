/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client.payment.builder.checkout;

import com.adyen.model.checkout.*;

import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.adyen.client.payment.builder.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckoutDetailsBuilder extends RequestBuilder<PaymentsDetailsRequest> {
    private final String merchantAccount;
    private final PaymentData paymentData;
    private final UserData userData;

    private static final Logger logger = LoggerFactory.getLogger(CheckoutDetailsBuilder.class);

    public CheckoutDetailsBuilder(final String merchantAccount,
                                  final PaymentData paymentData,
                                  final UserData userData) {
        super(new PaymentsDetailsRequest());
        this.merchantAccount = merchantAccount;
        this.paymentData = paymentData;
        this.userData = userData;
    }

    @Override
    public PaymentsDetailsRequest build() {
        KlarnaPaymentInfo paymentInfo = null;
        if (paymentData.getPaymentInfo() instanceof KlarnaPaymentInfo) {
            paymentInfo = (KlarnaPaymentInfo) paymentData.getPaymentInfo();
        } else {
            logger.error("Invalid paymentInfo object, expected KlarnaPaymentInfo");
            return request;
        }

        if(paymentInfo.getPaymentsData() != null) {
            request.setPaymentData(paymentInfo.getPaymentsData());
        }

        if(paymentInfo.getDetailsData() != null) {
            request.setDetails(paymentInfo.getDetailsData());
        }

        return request;
    }
}
