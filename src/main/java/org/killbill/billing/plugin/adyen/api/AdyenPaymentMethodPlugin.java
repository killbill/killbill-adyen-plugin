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

package org.killbill.billing.plugin.adyen.api;

import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;

import java.util.UUID;

public class AdyenPaymentMethodPlugin extends PluginPaymentMethodPlugin {

    public AdyenPaymentMethodPlugin(final AdyenPaymentMethodsRecord record) {
        super(UUID.fromString(record.getKbPaymentMethodId()),
                record.getToken(),
                (record.getIsDefault() != null) && AdyenDao.TRUE == record.getIsDefault(),
                AdyenModelPluginBase.buildPluginProperties(record.getAdditionalData()));
    }
}
