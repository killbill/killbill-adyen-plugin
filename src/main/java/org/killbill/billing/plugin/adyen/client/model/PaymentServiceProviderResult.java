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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;

public enum PaymentServiceProviderResult {

    AUTHORISED("Authorised"),
    CANCELLED("Cancelled"),
    CHALLENGE_SHOPPER("ChallengeShopper"), // possible authorize return code when using 3DS2.0
    ERROR(new String[]{"Error", "[error]"}),
    IDENTIFY_SHOPPER("IdentifyShopper"), // possible authorize return code when using 3DS2.0
    INITIALISED("Initialised"), // be careful with this state, it's only here to enable orders from OfflineFundsTransfer to be able to expire in every case after 7 days
    REFUSED("Refused"),
    PENDING("Pending"),
    RECEIVED(new String[]{"Received", "Pending", "[capture-received]", "[cancel-received]", "[cancelOrRefund-received]", "[refund-received]", "[all-details-successfully-disabled]", "[detail-successfully-disabled]"}), // direct debit, ideal payment response
    REDIRECT_SHOPPER("RedirectShopper"); // authorize return code when using 3DS1.0

    private static final Map<String, PaymentServiceProviderResult> REVERSE_LOOKUP = new HashMap<String, PaymentServiceProviderResult>();

    static {
        for (final PaymentServiceProviderResult providerResult : PaymentServiceProviderResult.values()) {
            for (final String response : providerResult.getResponses()) {
                REVERSE_LOOKUP.put(response, providerResult);
            }
        }
    }

    private final String[] responses;

    private PaymentServiceProviderResult(final String response) {
        this(new String[]{response});
    }

    private PaymentServiceProviderResult(final String[] responses) {
        this.responses = responses;
    }

    public static PaymentServiceProviderResult getPaymentResultForId(@Nullable final String id) {
        if (id == null) {
            return ERROR;
        }

        final PaymentServiceProviderResult result = REVERSE_LOOKUP.get(id);
        if (result != null) {
            return result;
        } else {
            // For HPP completion flow (see https://docs.adyen.com/developers/hpp-manual#hpppaymentresponse)
            return PaymentServiceProviderResult.valueOf(id);
        }
    }

    public static PaymentServiceProviderResult getPaymentResultForPluginStatus(@Nullable final PaymentPluginStatus paymentPluginStatus) {
        if (paymentPluginStatus == null) {
            return null;
        }

        switch (paymentPluginStatus) {
            case PROCESSED:
                return AUTHORISED;
            case PENDING:
                return PENDING;
            case ERROR:
                return REFUSED;
            case CANCELED:
                return ERROR;
            case UNDEFINED:
            default:
                return null;
        }
    }

    public String[] getResponses() {
        return responses;
    }

    @Override
    public String toString() {
        // Note! Needs to be compatible with getPaymentResultForId
        return this.responses[0];
    }
}
