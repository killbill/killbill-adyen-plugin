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

package org.killbill.billing.plugin.adyen.client.payment.builder;


import org.killbill.billing.plugin.adyen.client.model.RecurringType;
import org.testng.annotations.DataProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseTestPaymentRequestBuilder {

    protected static final String DP_RECURRING_TYPES = "RecurringTypes";

    @DataProvider(name = DP_RECURRING_TYPES)
    public Iterator<Object[]> recurringTypesdataProvider() {
        final List<Object[]> recurringTypes = new ArrayList<Object[]>(RecurringType.values().length);
        for (final RecurringType recurringType : RecurringType.values()) {
            recurringTypes.add(new Object[] {recurringType});
        }
        return recurringTypes.iterator();
    }

}
