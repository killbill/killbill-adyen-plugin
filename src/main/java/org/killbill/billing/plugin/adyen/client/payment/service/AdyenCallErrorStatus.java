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

package org.killbill.billing.plugin.adyen.client.payment.service;

public enum AdyenCallErrorStatus {
    /**
     * Request never reached Adyen (e.g. connection failure or unknown host).
     */
    REQUEST_NOT_SEND,
    /**
     * Adyens response indicates an invalid request sent by us (e.g. a non user field like pspRef was empty).
     */
    RESPONSE_ABOUT_INVALID_REQUEST,
    /**
     * We never received a response from Adyen.
     */
    RESPONSE_NOT_RECEIVED,
    /**
     * Received response is not parsable.
     */
    RESPONSE_INVALID,
    /**
     * We don't know ;).
     */
    UNKNOWN_FAILURE
}
