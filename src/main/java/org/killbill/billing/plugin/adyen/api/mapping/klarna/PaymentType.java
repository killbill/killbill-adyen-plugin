/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.api.mapping.klarna;

public enum PaymentType {
    PAY_LATER ("klarna"),
    PAY_NOW ("klarna_paynow"),
    PAY_INSTALLMENT ("klarna_account");

    private String value;
    PaymentType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
