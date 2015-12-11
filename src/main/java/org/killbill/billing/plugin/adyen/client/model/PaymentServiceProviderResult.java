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

import java.util.Arrays;

import javax.annotation.Nullable;

public enum PaymentServiceProviderResult {

    INITIALISED(new String[]{"Initialised"}), // be careful with this state, it's only here to enable orders from OfflineFundsTransfer to be able to expire in every case after 7 days
    AUTHORISED(new String[]{"Authorised"}),
    REDIRECT_SHOPPER(new String[]{"RedirectShopper"}), // authorize return code when using 3D-Secure
    RECEIVED(new String[]{"Received", "Pending", "[capture-received]", "[cancel-received]", "[cancelOrRefund-received]", "[refund-received]", "[all-details-successfully-disabled]", "[detail-successfully-disabled]"}), // direct debit, ideal payment response
    REFUSED(new String[]{"Refused"}),
    PENDING(new String[]{"Pending"}),
    ERROR(new String[]{"Error", "[error]"}),
    CANCELLED(new String[]{"Cancelled"});

    private final String[] responses;

    private PaymentServiceProviderResult(final String[] responses) {
        this.responses = responses;
    }

    public static PaymentServiceProviderResult getPaymentResultForId(@Nullable final String id) {
        if (id == null) {
            return ERROR;
        }

        for (final PaymentServiceProviderResult result : values()) {
            for (final String res : result.responses) {
                if (res.equalsIgnoreCase(id)) {
                    return result;
                }
            }
        }
        throw new IllegalArgumentException("Unknown PaymentResultType id: " + id);
    }

    @Override
    public String toString() {
        return Arrays.toString(this.responses);
    }
}
