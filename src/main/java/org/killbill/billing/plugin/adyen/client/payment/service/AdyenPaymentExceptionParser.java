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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.util.ArrayList;
import java.util.List;

import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderErrorCodes;

public class AdyenPaymentExceptionParser {

    private final static String INVALID_BANK_ACCOUNT_OR_ID = "Bank Account or Bank Location Id not valid or missing";
    private final static String INVALID_CARD_NUMBER = "Invalid card number";
    private final static String INVALID_EXPIRATION_DATE = "validation Invalid expiryMonth";
    private final static String INVALID_CARD_SECURITY_NUMBER = "CVC is not the right length";
    private final static String NO_LATEST_DETAIL_FOUND = "No latest detail found";

    public static List<PaymentServiceProviderErrorCodes> parsePaymentException(final String stackTrace) {
        final List<PaymentServiceProviderErrorCodes> errorList = new ArrayList<PaymentServiceProviderErrorCodes>();

        if (contains(stackTrace, INVALID_BANK_ACCOUNT_OR_ID)) {
            errorList.add(PaymentServiceProviderErrorCodes.BANK_CODE_OR_ACCOUNT_NUMBER);
        }

        if (contains(stackTrace, INVALID_CARD_NUMBER)) {
            errorList.add(PaymentServiceProviderErrorCodes.CARD_NUMBER);
        }

        if (contains(stackTrace, INVALID_EXPIRATION_DATE)) {
            errorList.add(PaymentServiceProviderErrorCodes.CARD_VALID_UNTIL);
        }

        if (contains(stackTrace, INVALID_CARD_SECURITY_NUMBER)) {
            errorList.add(PaymentServiceProviderErrorCodes.CARD_SECURITY_NUMBER);
        }

        return errorList;
    }

    public static boolean latestDetailNotFound(final String stackTrace) {
        return contains(stackTrace, NO_LATEST_DETAIL_FOUND);
    }

    private static boolean contains(final String seq, final String searchSeq) {
        return !(seq == null || searchSeq == null) && seq.indexOf(searchSeq, 0) >= 0;
    }

    private AdyenPaymentExceptionParser() {
    }
}
