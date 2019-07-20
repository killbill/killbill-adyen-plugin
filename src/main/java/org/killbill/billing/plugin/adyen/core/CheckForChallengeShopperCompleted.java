/*
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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

import java.util.UUID;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.api.PluginProperties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.PROPERTY_TRANS_STATUS;

public class CheckForChallengeShopperCompleted extends CheckForThreeDs2StepCompleted {

    @JsonCreator
    public CheckForChallengeShopperCompleted(@JsonProperty final UUID uuidKey,
                                             @JsonProperty final UUID kbTenantId,
                                             @JsonProperty final UUID kbPaymentMethodId,
                                             @JsonProperty final UUID kbPaymentId,
                                             @JsonProperty final UUID kbPaymentTransactionId,
                                             @JsonProperty final String kbPaymentTransactionExternalKey) {
        super(uuidKey, kbTenantId, kbPaymentMethodId, kbPaymentId, kbPaymentTransactionId, kbPaymentTransactionExternalKey);
    }

    @Override
    public void performAction(final AdyenPaymentPluginApi adyenPaymentPluginApi,
                              final AdyenDao adyenDao,
                              final OSGIKillbillAPI osgiKillbillAPI,
                              final AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler) throws Exception {
        performAction(
                PaymentServiceProviderResult.CHALLENGE_SHOPPER.name(),
                PluginProperties.buildPluginProperties(ImmutableMap.of(PROPERTY_TRANS_STATUS, "U")),
                "3d-secure: Authentication failed",
                adyenPaymentPluginApi,
                adyenDao,
                osgiKillbillAPI,
                adyenConfigPropertiesConfigurationHandler);
    }
}
