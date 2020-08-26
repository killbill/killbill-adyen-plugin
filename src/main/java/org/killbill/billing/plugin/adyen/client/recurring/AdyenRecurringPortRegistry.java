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

package org.killbill.billing.plugin.adyen.client.recurring;

import org.killbill.adyen.recurring.Recurring;
import org.killbill.adyen.recurring.RecurringPortType;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.AdyenPaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.jaxws.HttpHeaderInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingInInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingOutInterceptor;

public class AdyenRecurringPortRegistry extends AdyenPaymentPortRegistry implements RecurringPortRegistry {

    private static final String RECURRING_SERVICE_SUFFIX = "-recurringService";

    public AdyenRecurringPortRegistry(final AdyenConfigProperties config,
                                      final LoggingInInterceptor loggingInInterceptor,
                                      final LoggingOutInterceptor loggingOutInterceptor,
                                      final HttpHeaderInterceptor httpHeaderInterceptor) {
        super(config, loggingInInterceptor, loggingOutInterceptor, httpHeaderInterceptor);
    }

    @Override
    public RecurringPortType getRecurringPort(final String merchantAccount) {
        return createService(merchantAccount,
                             RECURRING_SERVICE_SUFFIX,
                             RecurringPortType.class,
                             Recurring.SERVICE,
                             Recurring.RecurringHttpPort,
                             config.getRecurringUrl(),
                             config.getRecurringConnectionTimeout(),
                             config.getRecurringReadTimeout());
    }
}
