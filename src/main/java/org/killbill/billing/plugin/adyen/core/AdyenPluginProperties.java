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

package org.killbill.billing.plugin.adyen.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AdyenPluginProperties {

  public enum PaymentMethodType {
    CARD
  }

  private static final Logger logger = LoggerFactory.getLogger(AdyenPluginProperties.class);
  public static final String PROPERTY_EXTERNAL_KEY = "paymentExternalKey";

  public static final String PROPERTY_AMOUNT = "amount";
  public static final String PROPERTY_CURRENCY = "currency";
  public static final String PROPERTY_DESCRIPTION = "description";

  public static final String PROPERTY_KB_ACCOUNT_ID = "kb_account_id";
  public static final String PROPERTY_KB_TRANSACTION_ID = "kb_transaction_id";
  public static final String PROPERTY_KB_PAYMENT_ID = "kb_payment_id";
  public static final String PROPERTY_KB_TRANSACTION_TYPE = "kb_transaction_type";

  public static Map<String, Object> toAdditionalDataMap(
      UUID kbPaymentId, UUID kbPaymentTransactionId) {
    final Map<String, Object> additionalDataMap = new HashMap<>();

    additionalDataMap.put(PROPERTY_KB_TRANSACTION_ID, kbPaymentTransactionId);
    additionalDataMap.put(PROPERTY_KB_PAYMENT_ID, kbPaymentId);

    return additionalDataMap;
  }

  public static Map<String, Object> toAdditionalDataMap(final String paymentMethodAdditionalData) {
    // JSON to Object
    final ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> additionalDataMap = null;
    try {
      additionalDataMap =
          mapper.readValue(
              paymentMethodAdditionalData, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      logger.error("{}", e.getMessage(), e);
    }
    return additionalDataMap;
  }

  public static String asString(final Map<?, ?> additionalData) throws SQLException {
    if (additionalData == null || additionalData.isEmpty()) {
      return null;
    }
    return asString((Object) additionalData);
  }

  public static String asString(final Object additionalData) throws SQLException {
    final ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.writeValueAsString(additionalData);
    } catch (final JsonProcessingException e) {
      throw new SQLException(e);
    }
  }
}
