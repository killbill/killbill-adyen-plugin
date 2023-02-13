/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
 *
 * Ning licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.plugin.adyen.api;

import java.util.List;
import java.util.UUID;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;
import org.killbill.billing.plugin.dao.PluginDao;

public class AdyenPaymentMethodPlugin extends PluginPaymentMethodPlugin {

  public static AdyenPaymentMethodPlugin build(
      final AdyenPaymentMethodsRecord adyenPaymentMethodsRecord) {

    return new AdyenPaymentMethodPlugin(
        UUID.fromString(adyenPaymentMethodsRecord.getKbPaymentMethodId()),
        null,
        adyenPaymentMethodsRecord.getIsDefault() == PluginDao.TRUE,
        PluginProperties.buildPluginProperties(
            AdyenDao.mapFromAdditionalDataString(adyenPaymentMethodsRecord.getAdditionalData())));
  }

  public AdyenPaymentMethodPlugin(
      final UUID kbPaymentMethodId,
      final String externalPaymentMethodId,
      final boolean isDefault,
      final List<PluginProperty> properties) {
    super(kbPaymentMethodId, externalPaymentMethodId, isDefault, properties);
  }
}
