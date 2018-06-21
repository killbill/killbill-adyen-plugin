/*
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.core;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.tenant.api.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * For the healthcheck to work with the main Kill Bill healthcheck, make sure to set in your global killbill.properties file:
 *
 *    org.killbill.billing.plugin.adyen.paymentUrl=https://pal-test.adyen.com/pal/servlet/Payment/v12
 */
public class AdyenHealthcheck implements Healthcheck {

    private static final Logger logger = LoggerFactory.getLogger(AdyenHealthcheck.class);

    private final AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler;

    public AdyenHealthcheck(final AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler) {
        this.adyenConfigPropertiesConfigurationHandler = adyenConfigPropertiesConfigurationHandler;
    }

    @Override
    public HealthStatus getHealthStatus(@Nullable final Tenant tenant, @Nullable final Map properties) {
        final AdyenConfigProperties adyenConfigProperties = adyenConfigPropertiesConfigurationHandler.getConfigurable(tenant == null ? null : tenant.getId());
        return pingAdyen(adyenConfigProperties);
    }

    private HealthStatus pingAdyen(final AdyenConfigProperties adyenConfigProperties) {
        final String paymentUrl = adyenConfigProperties.getPaymentUrl();

        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(paymentUrl).openConnection();
            connection.setRequestMethod("GET");

            if (adyenConfigProperties.getPaymentConnectionTimeout() != null) {
                connection.setConnectTimeout(Integer.valueOf(adyenConfigProperties.getPaymentConnectionTimeout()));
            }
            if (adyenConfigProperties.getPaymentReadTimeout() != null) {
                connection.setReadTimeout(Integer.valueOf(adyenConfigProperties.getPaymentReadTimeout()));
            }

            final int responseCode = connection.getResponseCode();
            final boolean healthy = responseCode == 401 || responseCode == 403; // Unauthorized, as no auth header was sent

            return healthy ? HealthStatus.healthy(paymentUrl + " OK") : HealthStatus.unHealthy(paymentUrl + " " + connection.getResponseMessage());
        } catch (final Exception exception) {
            logger.warn("Healthcheck failed", exception);
            return HealthStatus.unHealthy(paymentUrl + " " + (exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage()));
        }
    }
}
