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

import com.google.common.base.Strings;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;

public class AdyenPaymentTransactionInfoPlugin extends PluginPaymentTransactionInfoPlugin {

  // Kill Bill limits the field size to 32
  private static final int ERROR_CODE_MAX_LENGTH = 32;
  private static final String INTERNAL = "INTERNAL";
  private final AdyenResponsesRecord adyenResponseRecord;

  public static AdyenPaymentTransactionInfoPlugin build(
      final AdyenResponsesRecord AdyenResponsesRecord) {
    final Map<?, ?> additionalData =
        AdyenDao.mapFromAdditionalDataString(AdyenResponsesRecord.getAdditionalData());

    final String firstPaymentReferenceId = AdyenResponsesRecord.getPspReference();

    final DateTime responseDate =
        new DateTime(
            AdyenResponsesRecord.getCreatedDate().atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
            DateTimeZone.UTC);
    return new AdyenPaymentTransactionInfoPlugin(
        AdyenResponsesRecord,
        UUID.fromString(AdyenResponsesRecord.getKbPaymentId()),
        UUID.fromString(AdyenResponsesRecord.getKbPaymentTransactionId()),
        TransactionType.valueOf(AdyenResponsesRecord.getTransactionType()),
        AdyenResponsesRecord.getAmount(),
        Strings.isNullOrEmpty(AdyenResponsesRecord.getCurrency())
            ? null
            : Currency.valueOf(AdyenResponsesRecord.getCurrency()),
        null,
        AdyenResponsesRecord.getPspErrorCodes(),
        AdyenResponsesRecord.getPspErrorCodes(),
        firstPaymentReferenceId,
        null,
        responseDate,
        responseDate,
        PluginProperties.buildPluginProperties(additionalData));
  }

  public AdyenPaymentTransactionInfoPlugin(
      final AdyenResponsesRecord adyenResponsesRecord,
      final UUID kbPaymentId,
      final UUID kbTransactionPaymentPaymentId,
      final TransactionType transactionType,
      final BigDecimal amount,
      final Currency currency,
      final PaymentPluginStatus pluginStatus,
      final String gatewayError,
      final String gatewayErrorCode,
      final String firstPaymentReferenceId,
      final String secondPaymentReferenceId,
      final DateTime createdDate,
      final DateTime effectiveDate,
      final List<PluginProperty> properties) {
    super(
        kbPaymentId,
        kbTransactionPaymentPaymentId,
        transactionType,
        amount,
        currency,
        pluginStatus,
        gatewayError,
        gatewayErrorCode,
        firstPaymentReferenceId,
        secondPaymentReferenceId,
        createdDate,
        effectiveDate,
        properties);
    this.adyenResponseRecord = adyenResponsesRecord;
  }

  public AdyenPaymentTransactionInfoPlugin(
      AdyenResponsesRecord adyenRecord,
      UUID kbPaymentId,
      UUID fromString,
      TransactionType transactionType,
      BigDecimal amount,
      Currency valueOf,
      DateTime now2,
      ProcessorOutputDTO outputDTO) {
    super(
        kbPaymentId,
        fromString,
        transactionType,
        amount,
        valueOf,
        outputDTO.getStatus(),
        outputDTO.getGatewayError(),
        outputDTO.getGatewayErrorCode(),
        outputDTO.getFirstPaymentReferenceId(),
        outputDTO.getSecondPaymentReferenceId(),
        DateTime.now(),
        now2,
        outputDTO.getAdditionalData() != null
            ? mapToPluginPropertyList(outputDTO.getAdditionalData())
            : null);
    this.adyenResponseRecord = adyenRecord;
  }

  public AdyenResponsesRecord getAdyenResponseRecord() {
    return adyenResponseRecord;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    final AdyenPaymentTransactionInfoPlugin that = (AdyenPaymentTransactionInfoPlugin) o;

    return adyenResponseRecord != null
        ? adyenResponseRecord.equals(that.adyenResponseRecord)
        : (that.adyenResponseRecord == null);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (adyenResponseRecord != null ? adyenResponseRecord.hashCode() : 0);
    return result;
  }

  public static PaymentTransactionInfoPlugin unImplementedAPI(TransactionType transactionType) {

    return new AdyenPaymentTransactionInfoPlugin(
        null,
        null,
        null,
        transactionType,
        null,
        null,
        PaymentPluginStatus.CANCELED,
        "Method is not implemented",
        INTERNAL,
        null,
        null,
        null,
        null,
        null);
  }

  public static PaymentTransactionInfoPlugin cancelPaymentTransactionInfoPlugin(
      TransactionType transactionType, String message) {

    return new AdyenPaymentTransactionInfoPlugin(
        null,
        null,
        null,
        transactionType,
        null,
        null,
        PaymentPluginStatus.CANCELED,
        message,
        INTERNAL,
        null,
        null,
        null,
        null,
        null);
  }

  public static List<PluginProperty> mapToPluginPropertyList(Map<String, String> map) {
    List<PluginProperty> pluginList = new ArrayList<>();
    StringBuilder mapAsString = new StringBuilder();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      mapAsString.append(key + "=" + value + "&");
    }
    mapAsString.delete(mapAsString.length() - 1, mapAsString.length());
    pluginList.add(new PluginProperty("Response", mapAsString, true));
    return pluginList;
  }
}
