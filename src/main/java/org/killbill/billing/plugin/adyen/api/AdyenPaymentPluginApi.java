/*
 * Copyright 2021 Wovenware, Inc
 *
 * Wovenware licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.killbill.billing.plugin.adyen.api;

import static org.killbill.billing.plugin.adyen.core.resources.AdyenCheckoutService.IS_CHECKOUT;

import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.notification.NotificationHandler;
import com.adyen.util.HMACValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.api.exceptions.PaymentMethodException;
import org.killbill.billing.plugin.adyen.client.GatewayProcessor;
import org.killbill.billing.plugin.adyen.client.GatewayProcessorFactory;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenPaymentMethods;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenResponses;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginCallContext;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginGatewayNotification;
import org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdyenPaymentPluginApi
    extends PluginPaymentPluginApi<
        AdyenResponsesRecord, AdyenResponses, AdyenPaymentMethodsRecord, AdyenPaymentMethods> {

  private static final Logger logger = LoggerFactory.getLogger(AdyenPaymentPluginApi.class);
  public static final String INTERNAL = "INTERNAL";
  public static final String SESSION_DATA = "sessionData";
  public static final String RECURRING_DATA = "recurring.recurringDetailReference";
  public static final String ENABLE_RECURRING = "enableRecurring";
  protected static final ObjectMapper objectMapper = new ObjectMapper();
  private final AdyenConfigurationHandler adyenConfigurationHandler;
  private final AdyenDao adyenDao;

  public AdyenPaymentPluginApi(
      final AdyenConfigurationHandler adyenConfigPropertiesConfigurationHandler,
      final OSGIKillbillAPI killbillAPI,
      final OSGIConfigPropertiesService configProperties,
      final Clock clock,
      final AdyenDao dao) {
    super(killbillAPI, configProperties, clock, dao);
    this.adyenConfigurationHandler = adyenConfigPropertiesConfigurationHandler;
    this.adyenDao = dao;
  }

  @Override
  public List<PaymentTransactionInfoPlugin> getPaymentInfo(
      final UUID kbAccountId,
      final UUID kbPaymentId,
      final Iterable<PluginProperty> properties,
      final TenantContext context)
      throws PaymentPluginApiException {
    logger.info("[getPaymentInfo] getPaymentInfo for account {}", kbAccountId);
    final List<AdyenResponsesRecord> records;

    List<PaymentTransactionInfoPlugin> result = new ArrayList<>();
    try {

      records = this.adyenDao.getSuccessfulPurchaseResponseList(kbPaymentId, context.getTenantId());
      if (records == null || records.isEmpty()) {

        return new ArrayList<>();
      }
      for (AdyenResponsesRecord record : records) {
        Map<String, String> additionalDataMap =
            this.getAdditionalDataMap(record.getAdditionalData());
        List<PluginProperty> pluginProperty = this.mapToPluginPropertyList(additionalDataMap);
        PaymentTransactionInfoPlugin infoPlugin =
            new AdyenPaymentTransactionInfoPlugin(
                record,
                kbPaymentId,
                UUID.fromString(record.getKbPaymentTransactionId()),
                TransactionType.valueOf(record.getTransactionType()),
                record.getAmount(),
                record.getCurrency() != null ? Currency.valueOf(record.getCurrency()) : null,
                PaymentPluginStatus.valueOf(record.getTransactionStatus()),
                null,
                null,
                record.getSessionId(),
                null,
                DateTime.parse(record.getCreatedDate().toString()),
                DateTime.parse(record.getCreatedDate().toString()),
                pluginProperty);
        result.add(infoPlugin);
      }

    } catch (SQLException e) {
      logger.error("Error trying to access de DB ", e);
    }

    return result;
  }

  @Override
  protected PaymentTransactionInfoPlugin buildPaymentTransactionInfoPlugin(
      final AdyenResponsesRecord adyenRecord) {
    return AdyenPaymentTransactionInfoPlugin.build(adyenRecord);
  }

  @Override
  public PaymentMethodPlugin getPaymentMethodDetail(
      final UUID kbAccountId,
      final UUID kbPaymentMethodId,
      final Iterable<PluginProperty> properties,
      final TenantContext context)
      throws PaymentPluginApiException {
    logger.info("[getPaymentMethodDetail] Getting Payment Method Detail");
    AdyenPaymentMethodsRecord record;
    try {
      record = dao.getPaymentMethod(kbPaymentMethodId, context.getTenantId());

    } catch (final SQLException e) {
      logger.error(
          "[getPaymentMethodDetail] Unable to retrieve payment method for kbPaymentMethodId {} {} ",
          kbPaymentMethodId,
          e.getMessage());
      throw new PaymentPluginApiException(
          "Unable to retrieve payment method for kbPaymentMethodId " + kbPaymentMethodId, e);
    }

    if (record == null) {
      // return error if null
      return new AdyenPaymentMethodPlugin(
          kbPaymentMethodId, null, false, ImmutableList.<PluginProperty>of());
    } else {
      return buildPaymentMethodPlugin(record);
    }
  }

  @Override
  protected PaymentMethodPlugin buildPaymentMethodPlugin(
      final AdyenPaymentMethodsRecord adyenRecord) {
    return AdyenPaymentMethodPlugin.build(adyenRecord);
  }

  @Override
  protected PaymentMethodInfoPlugin buildPaymentMethodInfoPlugin(
      final AdyenPaymentMethodsRecord adyenRecord) {
    return AdyenPaymentMethodInfoPlugin.build(adyenRecord);
  }

  @Override
  public void addPaymentMethod(
      UUID kbAccountId,
      UUID kbPaymentMethodId,
      PaymentMethodPlugin paymentMethodProps,
      boolean setDefault,
      Iterable<PluginProperty> properties,
      CallContext context)
      throws PaymentPluginApiException {
    logger.info("[addPaymentMethod] Adding Payment Method");
    final Map<String, String> mergedProperties =
        PluginProperties.toStringMap(paymentMethodProps.getProperties(), properties);
    boolean recurring;
    if (this.adyenConfigurationHandler.getConfigurable(context.getTenantId()).getHMACKey() == null
        || this.adyenConfigurationHandler.getConfigurable(context.getTenantId()).getApiKey() == null
        || this.adyenConfigurationHandler
                .getConfigurable(context.getTenantId())
                .getMerchantAccount()
            == null
        || this.adyenConfigurationHandler.getConfigurable(context.getTenantId()).getReturnUrl()
            == null) {
      throw new PaymentMethodException(
          "Missing one or more configuration properties (HMAC KEY/ Api Key / Merchant Account / Return URL) ");
    }
    if (mergedProperties.get(ENABLE_RECURRING) != null
        && mergedProperties.get(ENABLE_RECURRING).equals("true")) {
      recurring = true;
    } else {
      recurring = false;
    }
    try {
      this.adyenDao.addPaymentMethod(
          kbAccountId,
          kbPaymentMethodId,
          mergedProperties,
          recurring,
          context.getTenantId(),
          setDefault);
    } catch (SQLException e) {

      throw new PaymentMethodException("[addPaymentMethod] Error inserting payment method", e);
    }
  }

  @Override
  protected String getPaymentMethodId(final AdyenPaymentMethodsRecord adyenRecord) {
    return adyenRecord.getKbPaymentMethodId();
  }

  @Override
  public void deletePaymentMethod(
      final UUID kbAccountId,
      final UUID kbPaymentMethodId,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    logger.info("[deletePaymentMethod] Deleting Payment Method");
    try {
      this.adyenDao.updateIsDeletePaymentMethod(kbPaymentMethodId, context.getTenantId());
    } catch (SQLException e) {
      logger.error("{}", e.getMessage(), e);
    }
    super.deletePaymentMethod(kbAccountId, kbPaymentMethodId, properties, context);
  }

  @Override
  public List<PaymentMethodInfoPlugin> getPaymentMethods(
      final UUID kbAccountId,
      final boolean refreshFromGateway,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    logger.info("[getPaymentMethods] Getting Payment Method");
    /* Disabled and returning only the methods in db. Normally, we would synch gateway payment methods with db payment methods */
    return super.getPaymentMethods(kbAccountId, false, properties, context);
  }

  @Override
  public PaymentTransactionInfoPlugin authorizePayment(
      final UUID kbAccountId,
      final UUID kbPaymentId,
      final UUID kbTransactionId,
      final UUID kbPaymentMethodId,
      final BigDecimal amount,
      final Currency currency,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    return AdyenPaymentTransactionInfoPlugin.unImplementedAPI(TransactionType.AUTHORIZE);
  }

  @Override
  public PaymentTransactionInfoPlugin capturePayment(
      final UUID kbAccountId,
      final UUID kbPaymentId,
      final UUID kbTransactionId,
      final UUID kbPaymentMethodId,
      final BigDecimal amount,
      final Currency currency,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    return AdyenPaymentTransactionInfoPlugin.unImplementedAPI(TransactionType.CAPTURE);
  }

  @Override
  public PaymentTransactionInfoPlugin purchasePayment(
      final UUID kbAccountId,
      final UUID kbPaymentId,
      final UUID kbTransactionId,
      final UUID kbPaymentMethodId,
      final BigDecimal amount,
      final Currency currency,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    logger.info("Purchase Payment for account {}", kbAccountId);
    final Map<String, String> mergedProperties = PluginProperties.toStringMap(properties);
    AdyenPaymentMethodsRecord paymentMethodRecord = null;
    try {
      paymentMethodRecord = this.adyenDao.getPaymentMethod(kbPaymentMethodId.toString());
    } catch (SQLException e1) {
      logger.error("[purchasePayment]  encountered a database error ", e1);
      return AdyenPaymentTransactionInfoPlugin.cancelPaymentTransactionInfoPlugin(
          TransactionType.PURCHASE, "[purchasePayment]  encountered a database error ");
    }
    GatewayProcessor gatewayProcessor =
        GatewayProcessorFactory.get(
            adyenConfigurationHandler.getConfigurable(context.getTenantId()));
    ProcessorInputDTO input =
        gatewayProcessor.validateData(
            adyenConfigurationHandler, mergedProperties, kbPaymentMethodId, kbAccountId);
    if (paymentMethodRecord.getIsRecurring() != 48) {
      input.setPaymentMethod(PaymentMethod.RECURRING);
    } else {
      input.setPaymentMethod(PaymentMethod.ONE_TIME);
    }
    input.setAmount(amount);
    input.setKbTransactionId(kbTransactionId.toString());
    input.setCurrency(currency);
    input.setKbAccountId(kbAccountId.toString());
    List<PluginProperty> formFields = new ArrayList<>();
    ProcessorOutputDTO outputDTO = null;
    if (mergedProperties.get(IS_CHECKOUT) != null
        && mergedProperties.get(IS_CHECKOUT).equals("true")) {
      outputDTO = gatewayProcessor.processPayment(input);
      formFields.add(
          new PluginProperty(SESSION_DATA, outputDTO.getAdditionalData().get(SESSION_DATA), false));
    } else {
      input.setRecurringData(paymentMethodRecord.getRecurringDetailReference());
      outputDTO = gatewayProcessor.processOneTimePayment(input);
    }

    AdyenResponsesRecord adyenRecord = null;
    try {
      adyenRecord =
          this.adyenDao.addResponse(
              kbAccountId,
              kbPaymentId,
              kbTransactionId,
              TransactionType.PURCHASE,
              amount,
              currency,
              PaymentPluginStatus.PENDING,
              outputDTO.getFirstPaymentReferenceId(),
              outputDTO,
              context.getTenantId());
    } catch (SQLException e) {
      logger.error("[purchasePayment]  encountered a database error ", e);
      return AdyenPaymentTransactionInfoPlugin.cancelPaymentTransactionInfoPlugin(
          TransactionType.PURCHASE, "[purchasePayment]  encountered a database error ");
    }
    return new AdyenPaymentTransactionInfoPlugin(
        adyenRecord,
        kbPaymentId,
        kbTransactionId,
        TransactionType.PURCHASE,
        amount,
        currency,
        PaymentPluginStatus.PENDING,
        null,
        null,
        outputDTO.getFirstPaymentReferenceId(),
        outputDTO.getSecondPaymentReferenceId(),
        DateTime.now(),
        DateTime.now(),
        formFields);
  }

  @Override
  public PaymentTransactionInfoPlugin voidPayment(
      final UUID kbAccountId,
      final UUID kbPaymentId,
      final UUID kbTransactionId,
      final UUID kbPaymentMethodId,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    logger.info("Refund Payment for account {}", kbAccountId);
    AdyenResponsesRecord adyenRecord = null;

    try {
      adyenRecord = this.adyenDao.getSuccessfulPurchaseResponse(kbPaymentId, context.getTenantId());
      if (adyenRecord == null) {
        logger.error("[voidPayment] Purchase do not exists");
        return AdyenPaymentTransactionInfoPlugin.cancelPaymentTransactionInfoPlugin(
            TransactionType.VOID, "Purchase do not exists");
      }

    } catch (SQLException e) {
      logger.error("[voidPayment]  but we encountered a database error", e);
      return AdyenPaymentTransactionInfoPlugin.cancelPaymentTransactionInfoPlugin(
          TransactionType.VOID, "[voidPayment] but we encountered a database error");
    }

    final Map<String, String> mergedProperties = PluginProperties.toStringMap(properties);
    GatewayProcessor gatewayProcessor =
        GatewayProcessorFactory.get(
            adyenConfigurationHandler.getConfigurable(context.getTenantId()));

    ProcessorInputDTO input =
        gatewayProcessor.validateData(
            adyenConfigurationHandler, mergedProperties, kbPaymentMethodId, kbAccountId);
    input.setPspReference(adyenRecord.getPspReference());

    input.setKbTransactionId(kbTransactionId.toString());

    ProcessorOutputDTO outputDTO = gatewayProcessor.voidPayment(input);

    try {
      adyenRecord =
          this.adyenDao.addResponse(
              kbAccountId,
              kbPaymentId,
              kbTransactionId,
              TransactionType.VOID,
              null,
              null,
              PaymentPluginStatus.PENDING,
              outputDTO.getFirstPaymentReferenceId(),
              outputDTO,
              context.getTenantId());
    } catch (SQLException e) {
      logger.error("We encountered a database error ", e);
    }
    return new AdyenPaymentTransactionInfoPlugin(
        adyenRecord,
        kbPaymentId,
        kbTransactionId,
        TransactionType.VOID,
        null,
        null,
        PaymentPluginStatus.PENDING,
        null,
        null,
        outputDTO.getFirstPaymentReferenceId(),
        outputDTO.getSecondPaymentReferenceId(),
        DateTime.now(),
        DateTime.now(),
        null);
  }

  @Override
  public PaymentTransactionInfoPlugin creditPayment(
      final UUID kbAccountId,
      final UUID kbPaymentId,
      final UUID kbTransactionId,
      final UUID kbPaymentMethodId,
      final BigDecimal amount,
      final Currency currency,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    return AdyenPaymentTransactionInfoPlugin.unImplementedAPI(TransactionType.CREDIT);
  }

  @Override
  public PaymentTransactionInfoPlugin refundPayment(
      final UUID kbAccountId,
      final UUID kbPaymentId,
      final UUID kbTransactionId,
      final UUID kbPaymentMethodId,
      final BigDecimal amount,
      final Currency currency,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    logger.info("Refund Payment for account {}", kbAccountId);
    AdyenResponsesRecord adyenRecord = null;

    try {
      adyenRecord = this.adyenDao.getSuccessfulPurchaseResponse(kbPaymentId, context.getTenantId());
      if (this.refundValidations(adyenRecord, amount) != null) {
        return this.refundValidations(adyenRecord, amount);
      }

    } catch (SQLException e) {
      logger.error("[refundPayment]  but we encountered a database error", e);
      return AdyenPaymentTransactionInfoPlugin.cancelPaymentTransactionInfoPlugin(
          TransactionType.REFUND, "[refundPayment] but we encountered a database error");
    }

    final Map<String, String> mergedProperties = PluginProperties.toStringMap(properties);
    GatewayProcessor gatewayProcessor =
        GatewayProcessorFactory.get(
            adyenConfigurationHandler.getConfigurable(context.getTenantId()));

    ProcessorInputDTO input =
        gatewayProcessor.validateData(
            adyenConfigurationHandler, mergedProperties, kbPaymentMethodId, kbAccountId);
    input.setPspReference(adyenRecord.getPspReference());
    input.setAmount(amount);
    input.setKbTransactionId(kbTransactionId.toString());
    input.setCurrency(currency);
    ProcessorOutputDTO outputDTO = gatewayProcessor.refundPayment(input);

    try {
      adyenRecord =
          this.adyenDao.addResponse(
              kbAccountId,
              kbPaymentId,
              kbTransactionId,
              TransactionType.REFUND,
              amount,
              currency,
              PaymentPluginStatus.PENDING,
              outputDTO.getFirstPaymentReferenceId(),
              outputDTO,
              context.getTenantId());
    } catch (SQLException e) {
      logger.error("We encountered a database error ", e);
    }
    return new AdyenPaymentTransactionInfoPlugin(
        adyenRecord,
        kbPaymentId,
        kbTransactionId,
        TransactionType.REFUND,
        amount,
        currency,
        PaymentPluginStatus.PENDING,
        null,
        null,
        outputDTO.getFirstPaymentReferenceId(),
        outputDTO.getSecondPaymentReferenceId(),
        DateTime.now(),
        DateTime.now(),
        null);
  }

  @Override
  public HostedPaymentPageFormDescriptor buildFormDescriptor(
      final UUID kbAccountId,
      final Iterable<PluginProperty> customFields,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    throw new PaymentPluginApiException(INTERNAL, "#buildFormDescriptor not implemented.");
  }

  @Override
  public GatewayNotification processNotification(
      final String notification,
      final Iterable<PluginProperty> properties,
      final CallContext context)
      throws PaymentPluginApiException {
    logger.info("Notification recieved");

    try {
      HMACValidator hmacValidator = new HMACValidator();
      NotificationHandler notificationHandler = new NotificationHandler();
      NotificationRequest notificationRequest =
          notificationHandler.handleNotificationJson(notification);

      NotificationRequestItem notificationItem = notificationRequest.getNotificationItems().get(0);

      AdyenResponsesRecord record =
          adyenDao.getResponseFromMerchantReference(notificationItem.getMerchantReference());
      final CallContext tempContext =
          new PluginCallContext(
              AdyenActivator.PLUGIN_NAME,
              clock.getUTCNow(),
              UUID.fromString(record.getKbAccountId()),
              UUID.fromString(record.getKbTenantId()));
      if (hmacValidator.validateHMAC(
          notificationItem,
          this.adyenConfigurationHandler.getConfigurable(tempContext.getTenantId()).getHMACKey())) {

        Payment payment =
            this.killbillAPI
                .getPaymentApi()
                .getPayment(
                    UUID.fromString(record.getKbPaymentId()),
                    false,
                    false,
                    properties,
                    tempContext);

        ProcessorOutputDTO outputDTO = new ProcessorOutputDTO();
        outputDTO.setPspReferenceCode(notificationItem.getPspReference());
        if (notificationItem.isSuccess()) {
          outputDTO.setStatus(PaymentPluginStatus.PROCESSED);
        } else {
          outputDTO.setStatus(PaymentPluginStatus.ERROR);
        }
        this.adyenDao.updateResponse(
            UUID.fromString(record.getKbPaymentId()),
            outputDTO,
            UUID.fromString(record.getKbTenantId()));
        this.adyenDao.addNotification(
            UUID.fromString(record.getKbAccountId()),
            UUID.fromString(record.getKbPaymentId()),
            UUID.fromString(record.getKbPaymentTransactionId()),
            notificationItem,
            UUID.fromString(record.getKbTenantId()));

        if (notificationItem.getAdditionalData().get(RECURRING_DATA) != null) {

          this.adyenDao.updateRecurringDetailsPaymentMethod(
              payment.getPaymentMethodId(),
              UUID.fromString(record.getKbTenantId()),
              notificationItem.getAdditionalData().get(RECURRING_DATA));
        }

      } else {
        logger.error("HMAC Key is not valid");
      }
    } catch (Exception e) {
      logger.error("{}", e.getMessage(), e);
    }
    return new PluginGatewayNotification("[accepted]");
  }

  public Map<String, String> getAdditionalDataMap(String additionalData) {
    if (additionalData == null) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(additionalData, Map.class);

    } catch (Exception e) {
      logger.error("", e);
    }
    return Collections.emptyMap();
  }

  public List<PluginProperty> mapToPluginPropertyList(Map<String, String> map) {
    List<PluginProperty> pluginList = new ArrayList<>();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      pluginList.add(new PluginProperty(key, value, true));
    }
    return pluginList;
  }

  public PaymentTransactionInfoPlugin refundValidations(
      AdyenResponsesRecord adyenRecord, BigDecimal amount) {
    if (adyenRecord == null) {
      logger.error("[refundPayment] Purchase do not exists");
      return AdyenPaymentTransactionInfoPlugin.cancelPaymentTransactionInfoPlugin(
          TransactionType.REFUND, "Purchase do not exists");
    }

    if (adyenRecord.getAmount().compareTo(amount) < 0) {
      logger.error("[refundPayment] The refund amount is more than the transaction amount");
      return AdyenPaymentTransactionInfoPlugin.cancelPaymentTransactionInfoPlugin(
          TransactionType.REFUND, "The refund amount is more than the transaction amount");
    }
    if (BigDecimal.ZERO.compareTo(amount) == 0) {
      logger.error("[refundPayment] The refund amount can not be zero");
      return AdyenPaymentTransactionInfoPlugin.cancelPaymentTransactionInfoPlugin(
          TransactionType.REFUND, "The refund amount can not be zero");
    } else {
      return null;
    }
  }
}
