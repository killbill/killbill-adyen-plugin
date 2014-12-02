/*
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client;

import java.util.HashMap;
import java.util.Map;

public enum AdyenEventCode {

    AUTHORISATION("AUTHORISATION"),
    CANCELLATION("CANCELLATION"),
    REFUND("REFUND"),
    CANCEL_OR_REFUND("CANCEL_OR_REFUND"),
    CAPTURE("CAPTURE"),
    CHARGEBACK("CHARGEBACK"),
    DISPUTE("DISPUTE"),
    REPORT_AVAILABLE("REPORT_AVAILABLE"),
    NOTIFICATIONTEST("NOTIFICATIONTEST"),
    CHARGEBACK_REVERSED("CHARGEBACK_REVERSED"),
    REQUEST_FOR_INFORMATION("REQUEST_FOR_INFORMATION"),
    NOTIFICATION_OF_CHARGEBACK("NOTIFICATION_OF_CHARGEBACK"),
    RECURRING_RECEIVED("[recurring-received]"),
    CANCEL_RECEIVED("[cancel-received]"),
    RECURRING_DETAIL_DISABLED("[detail-successfully-disabled]"),
    RECURRING_FOR_USER_DISABLED("[all-details-successfully-disabled]"),
    REFUND_FAILED("REFUND_FAILED"),
    REFUNDED_REVERSED("REFUNDED_REVERSED"),
    CAPTURE_FAILED("CAPTURE_FAILED"),
    NOTIFICATION_OF_FRAUD("NOTIFICATION_OF_FRAUD");

    private static final Map<String, AdyenEventCode> REVERSE_LOOKUP = new HashMap<String, AdyenEventCode>();

    static {
        for (final AdyenEventCode value : AdyenEventCode.values()) {
            REVERSE_LOOKUP.put(value.getId(), value);
        }
    }

    public static AdyenEventCode fromString(final String id) {
        return REVERSE_LOOKUP.get(id);
    }

    private final String id;

    private AdyenEventCode(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
