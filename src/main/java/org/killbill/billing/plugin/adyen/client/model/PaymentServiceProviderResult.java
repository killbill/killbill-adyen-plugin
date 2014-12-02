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

public enum PaymentServiceProviderResult {

    INITIALISED("Initialised"), // be careful with this state, it's only here to enable orders from OfflineFundsTransfer to be able to expire in every case after 7 days
    AUTHORISED("Authorised"),
    REDIRECT_SHOPPER("RedirectShopper"), // authorize return code when using 3D-Secure
    RECEIVED("Received"), // direct debit, ideal payment response
    REFUSED("Refused"),
    PENDING("Pending"),
    ERROR("Error"),
    CANCELLED("Cancelled");

    private final String id;

    private PaymentServiceProviderResult(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static PaymentServiceProviderResult getPaymentResultForId(final String id) {
        if (id.equalsIgnoreCase(AUTHORISED.getId())) {
            return AUTHORISED;
        } else if (id.equalsIgnoreCase(REFUSED.getId())) {
            return REFUSED;
        } else if (id.equalsIgnoreCase(RECEIVED.getId())) {
            return RECEIVED;
        } else if (id.equalsIgnoreCase(PENDING.getId())) {
            return PENDING;
        } else if (id.equalsIgnoreCase(ERROR.getId())) {
            return ERROR;
        } else if (id.equalsIgnoreCase(CANCELLED.getId())) {
            return CANCELLED;
        } else if (id.equalsIgnoreCase(REDIRECT_SHOPPER.getId())) {
            return REDIRECT_SHOPPER;
        } else if (id.equalsIgnoreCase(INITIALISED.getId())) {
            return INITIALISED;
        } else {
            throw new IllegalArgumentException("Unknown PaymentResultType id: " + id);
        }
    }

    @Override
    public String toString() {
        return this.id;
    }
}
