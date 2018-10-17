/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.api;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;

import org.joda.time.DateTime;
import org.killbill.adyen.recurring.RecurringDetail;
import org.killbill.adyen.recurring.ServiceException;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.api.mapping.PaymentInfoMappingService;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.HppCompletedResult;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData.Item;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.WebPaymentFrontend;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationHandler;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationService;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderHostedPaymentPagePort;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderPort;
import org.killbill.billing.plugin.adyen.client.recurring.AdyenRecurringClient;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.adyen.core.AdyenConfigPropertiesConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenHostedPaymentPageConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenRecurringConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.KillbillAdyenNotificationHandler;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenPaymentMethods;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenResponses;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenHppRequestsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi;
import org.killbill.billing.plugin.util.KillBillMoney;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static org.killbill.billing.plugin.adyen.api.mapping.UserDataMappingService.toUserData;

public class AdyenPaymentPluginApi extends PluginPaymentPluginApi<AdyenResponsesRecord, AdyenResponses, AdyenPaymentMethodsRecord, AdyenPaymentMethods> {

    // Shared properties
    public static final String PROPERTY_PAYMENT_PROCESSOR_ACCOUNT_ID = "paymentProcessorAccountId";
    public static final String PROPERTY_ACQUIRER = "acquirer";
    public static final String PROPERTY_ACQUIRER_MID = "acquirerMID";
    public static final String PROPERTY_SELECTED_BRAND = "selectedBrand";
    public static final String PROPERTY_INSTALLMENTS = "installments";
    public static final String SPLIT_SETTLEMENT_DATA_ITEM = "splitSettlementDataItem";
    public static final String PROPERTY_RECURRING_TYPE = "recurringType";
    public static final String PROPERTY_CAPTURE_DELAY_HOURS = "captureDelayHours";
    /**
     * Cont auth disabled validation on adyens side (no cvc required). We practically tell them that the payment data is valid.
     * Should be given as "true" or "false".
     */
    public static final String PROPERTY_CONTINUOUS_AUTHENTICATION = "contAuth";

    // API
    public static final String PROPERTY_RECURRING_DETAIL_ID = "recurringDetailId";

    // 3-D Secure
    public static final String PROPERTY_PA_RES = "PaRes";
    public static final String PROPERTY_MD = "MD";
    public static final String PROPERTY_TERM_URL = "TermUrl";
    public static final String PROPERTY_USER_AGENT = "userAgent";
    public static final String PROPERTY_ACCEPT_HEADER = "acceptHeader";
    public static final String PROPERTY_THREE_D_THRESHOLD = "threeDThreshold";
    public static final String PROPERTY_MPI_DATA_DIRECTORY_RESPONSE = "mpiDataDirectoryResponse";
    public static final String PROPERTY_MPI_DATA_AUTHENTICATION_RESPONSE = "mpiDataAuthenticationResponse";
    public static final String PROPERTY_MPI_DATA_CAVV = "mpiDataCavv";
    public static final String PROPERTY_MPI_DATA_CAVV_ALGORITHM = "mpiDataCavvAlgorithm";
    public static final String PROPERTY_MPI_DATA_XID = "mpiDataXid";
    public static final String PROPERTY_MPI_DATA_ECI = "mpiDataEci";
    public static final String PROPERTY_MPI_IMPLEMENTATION_TYPE = "mpiImplementationType";

    // Credit cards
    public static final String PROPERTY_CC_ISSUER_COUNTRY = "issuerCountry";
    public static final String PROPERTY_CC_ENCRYPTED_JSON = "encryptedJson";

    // SEPA
    public static final String PROPERTY_DD_HOLDER_NAME = "ddHolderName";
    public static final String PROPERTY_DD_ACCOUNT_NUMBER = "ddNumber";
    public static final String PROPERTY_DD_BANK_IDENTIFIER_CODE = "ddBic";
    // ELV only (processed as SEPA)
    public static final String PROPERTY_ELV_BLZ = "elvBlz";
    public static final String PROPERTY_SEPA_COUNTRY_CODE = "sepaCountryCode";

    // User data
    public static final String PROPERTY_FIRST_NAME = "firstName";
    public static final String PROPERTY_LAST_NAME = "lastName";
    public static final String PROPERTY_IP = "ip";
    public static final String PROPERTY_CUSTOMER_LOCALE = "customerLocale";
    public static final String PROPERTY_CUSTOMER_ID = "customerId";
    public static final String PROPERTY_EMAIL = "email";

    // HPP
    public static final String PROPERTY_CREATE_PENDING_PAYMENT = "createPendingPayment";
    public static final String PROPERTY_AUTH_MODE = "authMode";
    public static final String PROPERTY_PAYMENT_METHOD_ID = "paymentMethodId";
    public static final String PROPERTY_PAYMENT_EXTERNAL_KEY = "paymentExternalKey";
    public static final String PROPERTY_RESULT_URL = "resultUrl";
    public static final String PROPERTY_SERVER_URL = "serverUrl";
    public static final String PROPERTY_SHIP_BEFORE_DATE = "shipBeforeDate";
    public static final String PROPERTY_SKIN_CODE = "skin";
    public static final String PROPERTY_ORDER_DATA = "orderData";
    public static final String PROPERTY_SESSION_VALIDITY = "sessionValidity";
    public static final String PROPERTY_MERCHANT_RETURN_DATA = "merchantReturnData";
    public static final String PROPERTY_ALLOWED_METHODS = "allowedMethods";
    public static final String PROPERTY_BLOCKED_METHODS = "blockedMethods";
    public static final String PROPERTY_BRAND_CODE = "brandCode";
    public static final String PROPERTY_ISSUER_ID = "issuerId";
    public static final String PROPERTY_OFFER_EMAIL = "offerEmail";
    public static final String PROPERTY_HPP_TARGET = "hppTarget";
    public static final String PROPERTY_LOOKUP_DIRECTORY = "lookupDirectory";

    public static final String ADDITIONAL_DATA_ITEM = "additionalDataItem";

    // Internals
    public static final String PROPERTY_ADDITIONAL_DATA = "additionalData";
    public static final String PROPERTY_EVENT_CODE = "eventCode";
    public static final String PROPERTY_EVENT_DATE = "eventDate";
    public static final String PROPERTY_MERCHANT_ACCOUNT_CODE = "merchantAccountCode";
    public static final String PROPERTY_MERCHANT_REFERENCE = "merchantReference";
    public static final String PROPERTY_OPERATIONS = "operations";
    public static final String PROPERTY_ORIGINAL_REFERENCE = "originalReference";
    public static final String PROPERTY_PAYMENT_METHOD = "paymentMethod";
    public static final String PROPERTY_PSP_REFERENCE = "pspReference";
    public static final String PROPERTY_REASON = "reason";
    public static final String PROPERTY_SUCCESS = "success";
    public static final String PROPERTY_FROM_HPP = "fromHPP";
    public static final String PROPERTY_HPP_COMPLETION = "fromHPPCompletion";
    public static final String PROPERTY_FROM_HPP_TRANSACTION_STATUS = "fromHPPTransactionStatus";
    public static final String PROPERTY_PA_REQ = "PaReq";
    public static final String PROPERTY_DCC_AMOUNT_VALUE = "dccAmount";
    public static final String PROPERTY_DCC_AMOUNT_CURRENCY = "dccCurrency";
    public static final String PROPERTY_DCC_SIGNATURE = "dccSignature";
    public static final String PROPERTY_ISSUER_URL = "issuerUrl";

    private static final Logger logger = LoggerFactory.getLogger(AdyenPaymentPluginApi.class);

    private final AdyenConfigurationHandler adyenConfigurationHandler;
    private final AdyenHostedPaymentPageConfigurationHandler adyenHppConfigurationHandler;
    private final AdyenRecurringConfigurationHandler adyenRecurringConfigurationHandler;
    private final AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler;
    private final AdyenDao dao;
    private final AdyenNotificationService adyenNotificationService;

    public AdyenPaymentPluginApi(final AdyenConfigurationHandler adyenConfigurationHandler,
                                 final AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler,
                                 final AdyenHostedPaymentPageConfigurationHandler adyenHppConfigurationHandler,
                                 final AdyenRecurringConfigurationHandler adyenRecurringConfigurationHandler,
                                 final OSGIKillbillAPI killbillApi,
                                 final OSGIConfigPropertiesService osgiConfigPropertiesService,
                                 final OSGIKillbillLogService logService,
                                 final Clock clock,
                                 final AdyenDao dao) throws JAXBException {
        super(killbillApi, osgiConfigPropertiesService, logService, clock, dao);
        this.adyenConfigurationHandler = adyenConfigurationHandler;
        this.adyenHppConfigurationHandler = adyenHppConfigurationHandler;
        this.adyenRecurringConfigurationHandler = adyenRecurringConfigurationHandler;
        this.adyenConfigPropertiesConfigurationHandler = adyenConfigPropertiesConfigurationHandler;
        this.dao = dao;

        final AdyenNotificationHandler adyenNotificationHandler = new KillbillAdyenNotificationHandler(adyenConfigPropertiesConfigurationHandler, killbillApi, dao, clock);
        //noinspection RedundantTypeArguments
        this.adyenNotificationService = new AdyenNotificationService(ImmutableList.<AdyenNotificationHandler>of(adyenNotificationHandler));
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        final List<PaymentTransactionInfoPlugin> transactions = super.getPaymentInfo(kbAccountId, kbPaymentId, properties, context);
        if (transactions.isEmpty()) {
            // We don't know about this payment (maybe it was aborted in a control plugin)
            return transactions;
        }

        final ExpiredPaymentPolicy expiredPaymentPolicy = expiredPaymentPolicy(context);
        if (expiredPaymentPolicy.isExpired(transactions)) {
            cancelExpiredPayment(expiredPaymentPolicy.latestTransaction(transactions), context);
            // reload payment
            return super.getPaymentInfo(kbAccountId, kbPaymentId, properties, context);
        }
        return transactions;
    }

    private void cancelExpiredPayment(PaymentTransactionInfoPlugin expiredTransaction, final TenantContext context) {
        final List<PluginProperty> updatedStatusProperties = PluginProperties.buildPluginProperties(
                ImmutableMap.builder()
                            .put(PROPERTY_FROM_HPP_TRANSACTION_STATUS,
                                 PaymentPluginStatus.CANCELED.toString())
                            .put("message",
                                 "Payment Expired - Cancelled by Janitor")
                            .build());

        try {
            dao.updateResponse(expiredTransaction.getKbTransactionPaymentId(),
                               PluginProperties.merge(expiredTransaction.getProperties(), updatedStatusProperties),
                               context.getTenantId());
        } catch (final SQLException e) {
            logService.log(LogService.LOG_ERROR, "Unable to update canceled payment", e);
        }
    }

    private ExpiredPaymentPolicy expiredPaymentPolicy(final TenantContext context) {
        return new ExpiredPaymentPolicy(clock, getConfigProperties(context));
    }

    @Override
    protected PaymentTransactionInfoPlugin buildPaymentTransactionInfoPlugin(final AdyenResponsesRecord adyenResponsesRecord) {
        AdyenHppRequestsRecord hppRequestsRecord = null;
        try {
            hppRequestsRecord = dao.getHppRequest(UUID.fromString(adyenResponsesRecord.getKbPaymentTransactionId()));
        } catch (final SQLException e) {
            logger.warn("Unable to retrieve HPP request for paymentTransactionId='{}'", adyenResponsesRecord.getKbPaymentTransactionId(), e);
        }

        return new AdyenPaymentTransactionInfoPlugin(adyenResponsesRecord, hppRequestsRecord);
    }

    @Override
    protected PaymentMethodPlugin buildPaymentMethodPlugin(final AdyenPaymentMethodsRecord paymentMethodsRecord) {
        return new AdyenPaymentMethodPlugin(paymentMethodsRecord);
    }

    @Override
    protected PaymentMethodInfoPlugin buildPaymentMethodInfoPlugin(final AdyenPaymentMethodsRecord paymentMethodsRecord) {
        return new AdyenPaymentMethodInfoPlugin(paymentMethodsRecord);
    }

    @Override
    protected String getPaymentMethodId(final AdyenPaymentMethodsRecord paymentMethodsRecord) {
        return paymentMethodsRecord.getKbPaymentMethodId();
    }

    @Override
    public void deletePaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // Retrieve our currently known payment method
        final AdyenPaymentMethodsRecord adyenPaymentMethodsRecord;
        try {
             adyenPaymentMethodsRecord = dao.getPaymentMethod(kbPaymentMethodId, context.getTenantId());
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to retrieve payment method", e);
        }

        if (adyenPaymentMethodsRecord.getToken() != null) {
            // Retrieve the associated country for that shopper (and the corresponding merchant account)
            final Account account = getAccount(kbAccountId, context);
            final String pluginPropertyCountry = PluginProperties.findPluginPropertyValue(PROPERTY_COUNTRY, properties);
            final String countryCode = pluginPropertyCountry == null ? account.getCountry() : pluginPropertyCountry;
            final String merchantAccount = getMerchantAccount(countryCode, properties, context);

            final Map additionalData = AdyenDao.fromAdditionalData(adyenPaymentMethodsRecord.getAdditionalData());
            Object customerId = additionalData.get(PROPERTY_CUSTOMER_ID);
            if (customerId == null) {
                customerId = MoreObjects.firstNonNull(account.getExternalKey(), account.getId());
            }

            final AdyenRecurringClient adyenRecurringClient = adyenRecurringConfigurationHandler.getConfigurable(context.getTenantId());
            try {
                adyenRecurringClient.revokeRecurringDetails(customerId.toString(), merchantAccount);
            } catch (final ServiceException e) {
                throw new PaymentPluginApiException("Unable to revoke recurring details in Adyen", e);
            }
        }

        super.deletePaymentMethod(kbAccountId, kbPaymentMethodId, properties, context);
    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // If refreshFromGateway isn't set, simply read our tables
        if (!refreshFromGateway) {
            return super.getPaymentMethods(kbAccountId, refreshFromGateway, properties, context);
        }

        // Retrieve our currently known payment methods
        final List<AdyenPaymentMethodsRecord> existingPaymentMethods;
        try {
            existingPaymentMethods = dao.getPaymentMethods(kbAccountId, context.getTenantId());
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to retrieve existing payment methods", e);
        }

        // We cannot retrieve recurring details from Adyen without a shopper reference
        if (existingPaymentMethods.isEmpty()) {
            return super.getPaymentMethods(kbAccountId, refreshFromGateway, properties, context);
        }

        // Retrieve the associated country for that shopper (and the corresponding merchant account)
        final Account account = getAccount(kbAccountId, context);
        final String pluginPropertyCountry = PluginProperties.findPluginPropertyValue(PROPERTY_COUNTRY, properties);
        final String countryCode = pluginPropertyCountry == null ? account.getCountry() : pluginPropertyCountry;
        final String merchantAccount = getMerchantAccount(countryCode, properties, context);

        for (final AdyenPaymentMethodsRecord record : Lists.<AdyenPaymentMethodsRecord>reverse(existingPaymentMethods)) {
            if (record.getToken() != null) {
                // Immutable in Adyen -- nothing to do
                continue;
            }

            final Map additionalData = AdyenDao.fromAdditionalData(record.getAdditionalData());

            Object customerId = additionalData.get(PROPERTY_CUSTOMER_ID);
            if (customerId == null) {
                customerId = MoreObjects.firstNonNull(account.getExternalKey(), account.getId());
            }

            Object recurringType = additionalData.get(PROPERTY_RECURRING_TYPE);
            if (recurringType == null) {
                recurringType = MoreObjects.firstNonNull(PluginProperties.findPluginPropertyValue(PROPERTY_RECURRING_TYPE, properties), "RECURRING");
            }

            final AdyenRecurringClient adyenRecurringClient = adyenRecurringConfigurationHandler.getConfigurable(context.getTenantId());

            final List<RecurringDetail> recurringDetailList;
            try {
                recurringDetailList = adyenRecurringClient.getRecurringDetailList(customerId.toString(), merchantAccount, recurringType.toString());
            } catch (final ServiceException e) {
                logService.log(LogService.LOG_ERROR, "Unable to retrieve recurring details in Adyen", e);
                continue;
            }
            for (final RecurringDetail recurringDetail : recurringDetailList) {
                final AdyenResponsesRecord formerResponse;
                try {
                    formerResponse = dao.getResponse(recurringDetail.getFirstPspReference());
                } catch (final SQLException e) {
                    logService.log(LogService.LOG_ERROR, "Unable to retrieve adyen response", e);
                    continue;
                }
                if (formerResponse == null) {
                    continue;
                }

                final Payment payment;
                try {
                    payment = killbillAPI.getPaymentApi().getPayment(UUID.fromString(formerResponse.getKbPaymentId()), false, false, properties, context);
                } catch (final PaymentApiException e) {
                    logService.log(LogService.LOG_ERROR, "Unable to retrieve Payment for externalKey " + recurringDetail.getFirstPspReference(), e);
                    continue;
                }
                if (payment.getPaymentMethodId().toString().equals(record.getKbPaymentMethodId())) {
                    try {
                        dao.setPaymentMethodToken(record.getKbPaymentMethodId(), recurringDetail.getRecurringDetailReference(), context.getTenantId().toString());
                    } catch (final SQLException e) {
                        logService.log(LogService.LOG_ERROR, "Unable to update token", e);
                        continue;
                    }
                }
            }
        }

        return super.getPaymentMethods(kbAccountId, false, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final AdyenResponsesRecord adyenResponsesRecord = fetchResponseIfExist(kbPaymentId, context.getTenantId());
        final boolean isHPPCompletion = adyenResponsesRecord != null && Boolean.valueOf(MoreObjects.firstNonNull(AdyenDao.fromAdditionalData(adyenResponsesRecord.getAdditionalData()).get(PROPERTY_FROM_HPP), false).toString());
        if (!isHPPCompletion) {
            updateResponseWithAdditionalProperties(kbTransactionId, properties, context.getTenantId());
            // We don't have any record for that payment: we want to trigger an actual authorization call (or complete a 3D-S authorization)
            return executeInitialTransaction(TransactionType.AUTHORIZE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
        } else {
            // We already have a record for that payment transaction and we just updated the response row with additional properties
            // (the API can be called for instance after the user is redirected back from the HPP to store the PSP reference)
            updateResponseWithAdditionalProperties(kbTransactionId, PluginProperties.merge(ImmutableMap.of(PROPERTY_HPP_COMPLETION, true), properties), context.getTenantId());
        }

        return buildPaymentTransactionInfoPlugin(adyenResponsesRecord);
    }

    @Override
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.CAPTURE,
                                          new TransactionExecutor<PaymentModificationResponse>() {
                                              @Override
                                              public PaymentModificationResponse execute(final String merchantAccount, final PaymentData paymentData, final String pspReference, final SplitSettlementData splitSettlementData, final Map<String, String> additionalData) {
                                                  final AdyenPaymentServiceProviderPort port = adyenConfigurationHandler.getConfigurable(context.getTenantId());
                                                  return port.capture(merchantAccount, paymentData, pspReference, splitSettlementData, additionalData);
                                              }
                                          },
                                          kbAccountId,
                                          kbPaymentId,
                                          kbTransactionId,
                                          kbPaymentMethodId,
                                          amount,
                                          currency,
                                          properties,
                                          context);
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final AdyenResponsesRecord adyenResponsesRecord;
        try {
            adyenResponsesRecord = dao.updateResponse(kbTransactionId, properties, context.getTenantId());
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("HPP notification came through, but we encountered a database error", e);
        }

        if (adyenResponsesRecord == null) {
            // We don't have any record for that payment: we want to trigger an actual purchase (auto-capture) call
            final String captureDelayHours = PluginProperties.getValue(PROPERTY_CAPTURE_DELAY_HOURS, "0", properties);
            final Iterable<PluginProperty> overriddenProperties = PluginProperties.merge(properties, ImmutableList.<PluginProperty>of(new PluginProperty(PROPERTY_CAPTURE_DELAY_HOURS, captureDelayHours, false)));
            return executeInitialTransaction(TransactionType.PURCHASE, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, overriddenProperties, context);
        } else {
            // We already have a record for that payment transaction and we just updated the response row with additional properties
            // (the API can be called for instance after the user is redirected back from the HPP to store the PSP reference)
        }

        return buildPaymentTransactionInfoPlugin(adyenResponsesRecord);
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.VOID,
                                          new TransactionExecutor<PaymentModificationResponse>() {
                                              @Override
                                              public PaymentModificationResponse execute(final String merchantAccount, final PaymentData paymentData, final String pspReference, final SplitSettlementData splitSettlementData, final Map<String, String> additionalData) {
                                                  final AdyenPaymentServiceProviderPort port = adyenConfigurationHandler.getConfigurable(context.getTenantId());
                                                  return port.cancel(merchantAccount, paymentData, pspReference, splitSettlementData, additionalData);
                                              }
                                          },
                                          kbAccountId,
                                          kbPaymentId,
                                          kbTransactionId,
                                          kbPaymentMethodId,
                                          null,
                                          null,
                                          properties,
                                          context);
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // See https://docs.adyen.com/developers/api-manual#carddepositcardfundtransfercft
        return executeInitialTransaction(TransactionType.CREDIT, kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, properties, context);
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.REFUND,
                                          new TransactionExecutor<PaymentModificationResponse>() {
                                              @Override
                                              public PaymentModificationResponse execute(final String merchantAccount, final PaymentData paymentData, final String pspReference, final SplitSettlementData splitSettlementData, final Map<String, String> additionalData) {
                                                  final AdyenPaymentServiceProviderPort providerPort = adyenConfigurationHandler.getConfigurable(context.getTenantId());
                                                  return providerPort.refund(merchantAccount, paymentData, pspReference, splitSettlementData, additionalData);
                                              }
                                          },
                                          kbAccountId,
                                          kbPaymentId,
                                          kbTransactionId,
                                          kbPaymentMethodId,
                                          amount,
                                          currency,
                                          properties,
                                          context);
    }

    // HPP

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<PluginProperty> customFields, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        //noinspection unchecked
        final Iterable<PluginProperty> mergedProperties = PluginProperties.merge(customFields, properties);

        final Account account = getAccount(kbAccountId, context);
        final String countryCode = getCountryCode(account, null, properties);
        final String merchantAccount = getMerchantAccount(countryCode, properties, context);

        final String amountString = PluginProperties.findPluginPropertyValue(PROPERTY_AMOUNT, mergedProperties);
        Preconditions.checkState(!Strings.isNullOrEmpty(amountString), "amount not specified");
        final BigDecimal amount = new BigDecimal(amountString);
        final String currencyString = PluginProperties.findPluginPropertyValue(PROPERTY_CURRENCY, properties);
        final Currency currency = currencyString == null ? account.getCurrency() : Currency.valueOf(currencyString);
        Preconditions.checkState(currency != null, "currency not specified");

        final PaymentData paymentData = buildPaymentData(merchantAccount, countryCode, account, amount, currency, mergedProperties, context);
        final UserData userData = toUserData(account, mergedProperties);

        final boolean shouldCreatePendingPayment = Boolean.valueOf(PluginProperties.findPluginPropertyValue(PROPERTY_CREATE_PENDING_PAYMENT, mergedProperties));
        Payment pendingPayment = null;
        if (shouldCreatePendingPayment) {
            final boolean authMode = Boolean.valueOf(PluginProperties.findPluginPropertyValue(PROPERTY_AUTH_MODE, mergedProperties));
            final String paymentMethodIdString = PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_METHOD_ID, mergedProperties);
            final UUID paymentMethodId = paymentMethodIdString == null ? null : UUID.fromString(paymentMethodIdString);
            pendingPayment = createPendingPayment(authMode, account, paymentMethodId, paymentData, context);
        }

        final String merchantReference = pendingPayment == null ? paymentData.getPaymentTransactionExternalKey() : pendingPayment.getTransactions().get(0).getExternalKey();
        try {
            // Need to store on disk the mapping payment <-> user because Adyen's notification won't provide the latter
            //noinspection unchecked
            dao.addHppRequest(kbAccountId,
                              pendingPayment == null ? null : pendingPayment.getId(),
                              pendingPayment == null ? null : pendingPayment.getTransactions().get(0).getId(),
                              merchantReference,
                              propertiesToMapWithPropertyFiltering(mergedProperties, context),
                              clock.getUTCNow(),
                              context.getTenantId());
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to store HPP request", e);
        }

        final AdyenPaymentServiceProviderHostedPaymentPagePort hostedPaymentPagePort = adyenHppConfigurationHandler.getConfigurable(context.getTenantId());

        final SplitSettlementData splitSettlementData = buildSplitSettlementData(currency, properties);

        final Map formParameter;
        try {
            formParameter = hostedPaymentPagePort.getFormParameter(merchantAccount, paymentData, userData, splitSettlementData);
        } catch (final SignatureGenerationException e) {
            throw new PaymentPluginApiException("Unable to generate signature", e);
        }

        // Safe cast
        final WebPaymentFrontend webPaymentFrontend = (WebPaymentFrontend) paymentData.getPaymentInfo();

        final boolean withDirectory = Boolean.valueOf(PluginProperties.findPluginPropertyValue(PROPERTY_LOOKUP_DIRECTORY, mergedProperties));
        if (withDirectory) {
            final Map directory = hostedPaymentPagePort.getDirectory(merchantAccount,
                                                                     amount,
                                                                     currency,
                                                                     merchantReference,
                                                                     webPaymentFrontend.getSkinCode(),
                                                                     webPaymentFrontend.getSessionValidity(),
                                                                     paymentData.getPaymentInfo().getCountry());
            formParameter.put("directory", MoreObjects.firstNonNull(directory, ImmutableMap.of()));
        }

        final String target = webPaymentFrontend.getBrandCode() != null && webPaymentFrontend.getIssuerId() != null ? getConfigProperties(context).getHppSkipDetailsTarget() : getConfigProperties(context).getHppTarget();
        final String hppTarget = PluginProperties.getValue(PROPERTY_HPP_TARGET, target, properties);
        return new AdyenHostedPaymentPageFormDescriptor(kbAccountId, hppTarget, PluginProperties.buildPluginProperties(formParameter));
    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final String notificationResponse = adyenNotificationService.handleNotifications(notification);
        return new AdyenGatewayNotification(notificationResponse);
    }

    private abstract static class TransactionExecutor<T> {

        public T execute(final String merchantAccount, final PaymentData paymentData, final UserData userData, final SplitSettlementData splitSettlementData, final Map<String, String> additionalData) {
            throw new UnsupportedOperationException();
        }

        public T execute(final String merchantAccount, final PaymentData paymentData, final String pspReference, final SplitSettlementData splitSettlementData, final Map<String, String> additionalData) {
            throw new UnsupportedOperationException();
        }
    }

    private final Map<String, Object> propertiesToMapWithPropertyFiltering(final Iterable<PluginProperty> properties, TenantContext context) {
        final Map<String, Object> map = PluginProperties.toMap(properties);
        final List<String> sensitiveKeys = getConfigProperties(context).getSensitivePropertyKeys();
        for (final String sensitiveKey : sensitiveKeys) {
            map.remove(sensitiveKey);
        }
        return map;
    }

    private PaymentTransactionInfoPlugin executeInitialTransaction(final TransactionType transactionType,
                                                                   final UUID kbAccountId,
                                                                   final UUID kbPaymentId,
                                                                   final UUID kbTransactionId,
                                                                   final UUID kbPaymentMethodId,
                                                                   final BigDecimal amount,
                                                                   final Currency currency,
                                                                   final Iterable<PluginProperty> properties,
                                                                   final CallContext context) throws PaymentPluginApiException {
        return executeInitialTransaction(transactionType,
                                         new TransactionExecutor<PurchaseResult>() {
                                             @Override
                                             public PurchaseResult execute(final String merchantAccount, final PaymentData paymentData, final UserData userData, final SplitSettlementData splitSettlementData, final Map<String, String> additionalData) {
                                                 final AdyenPaymentServiceProviderPort adyenPort = adyenConfigurationHandler.getConfigurable(context.getTenantId());
                                                 final AdyenResponsesRecord existingAuth = previousAdyenResponseRecord(kbPaymentId, kbTransactionId.toString(), context);
                                                 if (existingAuth != null) {
                                                     // We are completing a 3D-S payment
                                                     final String originalMerchantAccount = getMerchantAccountFromRecord(existingAuth);
                                                     return adyenPort.authorize3DSecure(originalMerchantAccount != null? originalMerchantAccount: merchantAccount, paymentData, userData, splitSettlementData, additionalData);
                                                 } else {
                                                     // We are creating a new transaction (AUTHORIZE, PURCHASE or CREDIT)
                                                     if (transactionType == TransactionType.CREDIT) {
                                                         return adyenPort.credit(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
                                                     } else {
                                                         return adyenPort.authorise(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
                                                     }
                                                 }
                                             }
                                         },
                                         kbAccountId,
                                         kbPaymentId,
                                         kbTransactionId,
                                         kbPaymentMethodId,
                                         amount,
                                         currency,
                                         properties,
                                         context);
    }

    private PaymentTransactionInfoPlugin executeInitialTransaction(final TransactionType transactionType,
                                                                   final TransactionExecutor<PurchaseResult> transactionExecutor,
                                                                   final UUID kbAccountId,
                                                                   final UUID kbPaymentId,
                                                                   final UUID kbTransactionId,
                                                                   final UUID kbPaymentMethodId,
                                                                   final BigDecimal amount,
                                                                   final Currency currency,
                                                                   final Iterable<PluginProperty> properties,
                                                                   final TenantContext context) throws PaymentPluginApiException {
        final Account account = getAccount(kbAccountId, context);
        final AdyenPaymentMethodsRecord nonNullPaymentMethodsRecord = getAdyenPaymentMethodsRecord(kbPaymentMethodId, context);
        final String countryCode = getCountryCode(account, nonNullPaymentMethodsRecord, properties);
        final String merchantAccount = getMerchantAccount(countryCode, properties, context);

        final boolean fromHPP = Boolean.valueOf(PluginProperties.findPluginPropertyValue(PROPERTY_FROM_HPP, properties));
        if (fromHPP) {
            // We are either processing a notification (see KillbillAdyenNotificationHandler), creating a PENDING payment for HPP (see buildFormDescriptor) or recording a payment post HPP redirect
            return getPaymentTransactionInfoPluginForHPP(transactionType, kbAccountId, kbPaymentId, kbTransactionId, amount, currency, properties, context);
        }

        // Pull extra properties from the payment method (such as the customerId)
        final Iterable<PluginProperty> additionalPropertiesFromRecord = buildPaymentMethodPlugin(nonNullPaymentMethodsRecord).getProperties();
        //noinspection unchecked
        final Iterable<PluginProperty> mergedProperties = PluginProperties.merge(additionalPropertiesFromRecord, properties);
        final PaymentData paymentData = buildPaymentData(merchantAccount, countryCode, account, kbPaymentId, kbTransactionId, nonNullPaymentMethodsRecord, amount, currency, mergedProperties, context);
        final UserData userData = toUserData(account, mergedProperties);
        final SplitSettlementData splitSettlementData = buildSplitSettlementData(currency, properties);
        final Map<String, String> additionalData = buildAdditionalData(properties);
        final DateTime utcNow = clock.getUTCNow();

        final PurchaseResult response;
        if (shouldSkipAdyen(properties)) {
            response = new PurchaseResult(PaymentServiceProviderResult.AUTHORISED,
                                          null,
                                          PluginProperties.findPluginPropertyValue(PROPERTY_PSP_REFERENCE, properties),
                                          "skip_gw",
                                          PaymentServiceProviderResult.AUTHORISED.getResponses()[0],
                                          paymentData.getPaymentTransactionExternalKey(),
                                          ImmutableMap.<String, String>of("skipGw", "true",
                                                                          AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE, merchantAccount,
                                                                          "merchantReference", paymentData.getPaymentTransactionExternalKey(),
                                                                          "fromHPPTransactionStatus", "PROCESSED"));
        } else {
            response = transactionExecutor.execute(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
        }

        try {
            dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, transactionType, amount, currency, response, utcNow, context.getTenantId());
            return new AdyenPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, transactionType, amount, currency, utcNow, response);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Payment went through, but we encountered a database error. Payment details: " + response.toString(), e);
        }
    }

    private PaymentTransactionInfoPlugin executeFollowUpTransaction(final TransactionType transactionType,
                                                                    final TransactionExecutor<PaymentModificationResponse> transactionExecutor,
                                                                    final UUID kbAccountId,
                                                                    final UUID kbPaymentId,
                                                                    final UUID kbTransactionId,
                                                                    final UUID kbPaymentMethodId,
                                                                    @Nullable final BigDecimal amount,
                                                                    @Nullable final Currency currency,
                                                                    final Iterable<PluginProperty> properties,
                                                                    final TenantContext context) throws PaymentPluginApiException {
        final Account account = getAccount(kbAccountId, context);
        final AdyenPaymentMethodsRecord nonNullPaymentMethodsRecord = getAdyenPaymentMethodsRecord(kbPaymentMethodId, context);
        final String countryCode = getCountryCode(account, nonNullPaymentMethodsRecord, properties);

        final boolean fromHPP = Boolean.valueOf(PluginProperties.findPluginPropertyValue(PROPERTY_FROM_HPP, properties));
        if (fromHPP) {
            // We are processing a notification (see KillbillAdyenNotificationHandler)
            return getPaymentTransactionInfoPluginForHPP(transactionType, kbAccountId, kbPaymentId, kbTransactionId, amount, currency, properties, context);
        }

        final AdyenResponsesRecord previousResponse;
        try {
            previousResponse = dao.getSuccessfulAuthorizationResponse(kbPaymentId, context.getTenantId());
            if (previousResponse == null) {
                throw new PaymentPluginApiException(null, "Unable to retrieve previous payment response for kbTransactionId " + kbTransactionId);
            }
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to retrieve previous payment response for kbTransactionId " + kbTransactionId, e);
        }

        final String merchantAccount = getMerchantAccount(countryCode, previousResponse, properties, context);
        final PaymentData paymentData = buildPaymentData(merchantAccount, countryCode, account, kbPaymentId, kbTransactionId, nonNullPaymentMethodsRecord, amount, currency, properties, context);
        final SplitSettlementData splitSettlementData = buildSplitSettlementData(currency, properties);
        final Map<String, String> additionalData = buildAdditionalData(properties);
        final DateTime utcNow = clock.getUTCNow();

        final PaymentModificationResponse response;
        if (shouldSkipAdyen(properties)) {
            response = new PaymentModificationResponse(PaymentServiceProviderResult.PENDING.getResponses()[0],
                                                       PluginProperties.findPluginPropertyValue(PROPERTY_PSP_REFERENCE, properties),
                                                       ImmutableMap.<Object, Object>of("skipGw", "true",
                                                                                       "merchantAccountCode", merchantAccount,
                                                                                       "merchantReference", paymentData.getPaymentTransactionExternalKey(),
                                                                                       "fromHPPTransactionStatus", "PROCESSED"));
        } else {
            response = transactionExecutor.execute(merchantAccount, paymentData, previousResponse.getPspReference(), splitSettlementData, additionalData);
        }

        final Optional<PaymentServiceProviderResult> paymentServiceProviderResult;
        if (response.isTechnicallySuccessful()) {
            paymentServiceProviderResult = Optional.of(PaymentServiceProviderResult.RECEIVED);
        } else {
            paymentServiceProviderResult = Optional.<PaymentServiceProviderResult>absent();
        }

        try {
            dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, transactionType, amount, currency, response, utcNow, context.getTenantId());
            return new AdyenPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, transactionType, amount, currency, paymentServiceProviderResult, utcNow, response);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Payment went through, but we encountered a database error. Payment details: " + (response.toString()), e);
        }
    }

    private void updateResponseWithAdditionalProperties(final UUID kbTransactionId, final Iterable<PluginProperty> properties, final UUID tenantId) throws PaymentPluginApiException {
        try {
            dao.updateResponse(kbTransactionId, properties, tenantId);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("SQL exception when updating response", e);
        }
    }

    private AdyenResponsesRecord fetchResponseIfExist(final UUID kbPaymentId, final UUID tenantId) throws PaymentPluginApiException {
        try {
            return dao.getSuccessfulAuthorizationResponse(kbPaymentId, tenantId);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("SQL exception when fetching response", e);
        }
    }

    private boolean shouldSkipAdyen(final Iterable<PluginProperty> properties) {
        return "true".equals(PluginProperties.findPluginPropertyValue("skipGw", properties)) || "true".equals(PluginProperties.findPluginPropertyValue("skip_gw", properties));
    }

    private String getCountryCode(final AccountData account, @Nullable final AdyenPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties) {
        String country = PluginProperties.getValue(PROPERTY_COUNTRY, paymentMethodsRecord == null ? null : paymentMethodsRecord.getCountry(), properties);
        if (country == null && account != null) {
            country = account.getCountry();
        }
        return country;
    }

    private SplitSettlementData buildSplitSettlementData(final Currency currency, final Iterable<PluginProperty> pluginProperties) {
        final Map<Short, BigDecimal> amounts = new HashMap<Short, BigDecimal>();
        final Map<Short, String> groups = new HashMap<Short, String>();
        final Map<Short, String> references = new HashMap<Short, String>();
        final Map<Short, String> types = new HashMap<Short, String>();
        for (final PluginProperty pluginProperty : pluginProperties) {
            if (pluginProperty.getKey().startsWith(SPLIT_SETTLEMENT_DATA_ITEM) && pluginProperty.getValue() != null) {
                final String[] parts = pluginProperty.getKey().split("\\.");
                final Short itemNb = Short.parseShort(parts[1]);
                final String suffix = parts[2];

                final String value = pluginProperty.getValue().toString();
                if ("amount".equals(suffix)) {
                    // In major units
                    amounts.put(itemNb, new BigDecimal(value));
                } else if ("group".equals(suffix)) {
                    groups.put(itemNb, value);
                } else if ("reference".equals(suffix)) {
                    references.put(itemNb, value);
                } else if ("type".equals(suffix)) {
                    types.put(itemNb, value);
                }
            }
        }

        final List<Item> items = new LinkedList<Item>();
        for (final Short itemNb : amounts.keySet()) {
            final String type = types.get(itemNb);
            if (type != null) {
                items.add(new SplitSettlementData.Item(KillBillMoney.toMinorUnits(currency.toString(), amounts.get(itemNb)),
                                                       MoreObjects.firstNonNull(groups.get(itemNb), type),
                                                       MoreObjects.firstNonNull(references.get(itemNb), type),
                                                       type));
            }
        }

        if (items.isEmpty()) {
            return null;
        } else {
            return new SplitSettlementData(1, currency.toString(), items);
        }
    }

    private Map<String, String> buildAdditionalData(final Iterable<PluginProperty> pluginProperties) {
        final Map<Short, String> keys = new HashMap<Short, String>();
        final Map<Short, String> values = new HashMap<Short, String>();
        for (final PluginProperty pluginProperty : pluginProperties) {
            if (pluginProperty.getKey().startsWith(ADDITIONAL_DATA_ITEM) && pluginProperty.getValue() != null) {
                final String[] parts = pluginProperty.getKey().split("\\.");
                final Short itemNb = Short.parseShort(parts[1]);
                final String suffix = parts[2];

                final String value = pluginProperty.getValue().toString();
                if ("key".equals(suffix)) {
                    keys.put(itemNb, value);
                } else if ("value".equals(suffix)) {
                    values.put(itemNb, value);
                }
            }
        }

        final Map<String, String> additionalData = new HashMap<String, String>();
        for (final Short itemNb : keys.keySet()) {
            additionalData.put(keys.get(itemNb), values.get(itemNb));
        }

        return additionalData;
    }

    private PaymentTransactionInfoPlugin getPaymentTransactionInfoPluginForHPP(final TransactionType transactionType, final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        final AdyenPaymentServiceProviderHostedPaymentPagePort hostedPaymentPagePort = adyenHppConfigurationHandler.getConfigurable(context.getTenantId());
        final Map<String, String> requestParameterMap = Maps.transformValues(PluginProperties.toStringMap(properties), new Function<String, String>() {
            @Override
            public String apply(final String input) {
                // Adyen will encode parameters like merchantSig
                return decode(input);
            }
        });
        final HppCompletedResult hppCompletedResult = hostedPaymentPagePort.parseAndVerifyRequestIntegrity(requestParameterMap);
        final PurchaseResult purchaseResult = new PurchaseResult(hppCompletedResult);

        final DateTime utcNow = clock.getUTCNow();
        try {
            final AdyenResponsesRecord adyenResponsesRecord = dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, transactionType, amount, currency, purchaseResult, utcNow, context.getTenantId());
            return buildPaymentTransactionInfoPlugin(adyenResponsesRecord);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("HPP payment came through, but we encountered a database error", e);
        }
    }

    // For API
    private PaymentData buildPaymentData(final String merchantAccount, final String countryCode, final AccountData account, final UUID kbPaymentId, final UUID kbTransactionId, final AdyenPaymentMethodsRecord paymentMethodsRecord, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        final Payment payment;
        try {
            payment = killbillAPI.getPaymentApi().getPayment(kbPaymentId, false, false, properties, context);
        } catch (final PaymentApiException e) {
            throw new PaymentPluginApiException(String.format("Unable to retrieve kbPaymentId='%s'", kbPaymentId), e);
        }

        final PaymentTransaction paymentTransaction = Iterables.<PaymentTransaction>find(payment.getTransactions(),
                                                                                         new Predicate<PaymentTransaction>() {
                                                                                             @Override
                                                                                             public boolean apply(final PaymentTransaction input) {
                                                                                                 return kbTransactionId.equals(input.getId());
                                                                                             }
                                                                                         });

        final PaymentInfo paymentInfo = buildPaymentInfo(merchantAccount, countryCode, account, paymentMethodsRecord, properties, context);

        return new PaymentData<PaymentInfo>(amount, currency, paymentTransaction.getExternalKey(), paymentInfo);
    }

    // For HPP
    private PaymentData buildPaymentData(final String merchantAccount, final String countryCode, final AccountData account, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final TenantContext context) {
        final PaymentInfo paymentInfo = buildPaymentInfo(merchantAccount, countryCode, account, null, properties, context);
        final String paymentTransactionExternalKey = PluginProperties.getValue(PROPERTY_PAYMENT_EXTERNAL_KEY, UUID.randomUUID().toString(), properties);
        return new PaymentData<PaymentInfo>(amount, currency, paymentTransactionExternalKey, paymentInfo);
    }

    private PaymentInfo buildPaymentInfo(final String merchantAccount, final String countryCode, final AccountData account, @Nullable final AdyenPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties, final TenantContext context) {
        // A bit of a hack - it would be nice to be able to isolate AdyenConfigProperties
        final AdyenConfigProperties adyenConfigProperties = getConfigProperties(context);
        return PaymentInfoMappingService.toPaymentInfo(merchantAccount, countryCode, adyenConfigProperties, clock, account, paymentMethodsRecord, properties);
    }

    /**
     * There is the option not to use the adyen payment method table to retrieve payment data but to always provide
     * it as plugin properties. In this case an empty record (null object) could help.
     */
    private AdyenPaymentMethodsRecord emptyRecord(@Nullable final UUID kbPaymentMethodId) {
        final AdyenPaymentMethodsRecord record = new AdyenPaymentMethodsRecord();
        if (kbPaymentMethodId != null) {
            record.setKbPaymentMethodId(kbPaymentMethodId.toString());
        }
        return record;
    }

    private AdyenPaymentMethodsRecord getAdyenPaymentMethodsRecord(@Nullable final UUID kbPaymentMethodId, final TenantContext context) {
        AdyenPaymentMethodsRecord paymentMethodsRecord = null;

        if (kbPaymentMethodId != null) {
            try {
                paymentMethodsRecord = dao.getPaymentMethod(kbPaymentMethodId, context.getTenantId());
            } catch (final SQLException e) {
                logService.log(LogService.LOG_WARNING, "Failed to retrieve payment method " + kbPaymentMethodId, e);
            }
        }

        return MoreObjects.firstNonNull(paymentMethodsRecord, emptyRecord(kbPaymentMethodId));
    }

    private Payment createPendingPayment(final boolean authMode, final Account account, @Nullable final UUID paymentMethodId, final PaymentData paymentData, final CallContext context) throws PaymentPluginApiException {
        final UUID kbPaymentId = null;
        final String paymentTransactionExternalKey = paymentData.getPaymentTransactionExternalKey();
        //noinspection UnnecessaryLocalVariable
        final String paymentExternalKey = paymentTransactionExternalKey;
        final ImmutableMap<String, Object> purchasePropertiesMap = ImmutableMap.<String, Object>of(AdyenPaymentPluginApi.PROPERTY_FROM_HPP, true,
                                                                                                   AdyenPaymentPluginApi.PROPERTY_FROM_HPP_TRANSACTION_STATUS, PaymentPluginStatus.PENDING.toString());
        final Iterable<PluginProperty> purchaseProperties = PluginProperties.buildPluginProperties(purchasePropertiesMap);

        try {
            final UUID kbPaymentMethodId = paymentMethodId != null ? paymentMethodId : getAdyenKbPaymentMethodId(account.getId(), context);
            if (authMode) {
                return killbillAPI.getPaymentApi().createAuthorization(account,
                                                                       kbPaymentMethodId,
                                                                       kbPaymentId,
                                                                       paymentData.getAmount(),
                                                                       paymentData.getCurrency(),
                                                                       clock.getUTCNow(),
                                                                       paymentExternalKey,
                                                                       paymentTransactionExternalKey,
                                                                       purchaseProperties,
                                                                       context);
            } else {
                return killbillAPI.getPaymentApi().createPurchase(account,
                                                                  kbPaymentMethodId,
                                                                  kbPaymentId,
                                                                  paymentData.getAmount(),
                                                                  paymentData.getCurrency(),
                                                                  clock.getUTCNow(),
                                                                  paymentExternalKey,
                                                                  paymentTransactionExternalKey,
                                                                  purchaseProperties,
                                                                  context);
            }
        } catch (final PaymentApiException e) {
            throw new PaymentPluginApiException("Failed to record purchase", e);
        }
    }

    // Could be shared (see KillbillAdyenNotificationHandler)
    private UUID getAdyenKbPaymentMethodId(final UUID kbAccountId, final TenantContext context) throws PaymentApiException {
        //noinspection RedundantTypeArguments
        return Iterables.<PaymentMethod>find(killbillAPI.getPaymentApi().getAccountPaymentMethods(kbAccountId, false, false, ImmutableList.<PluginProperty>of(), context),
                              new Predicate<PaymentMethod>() {
                                  @Override
                                  public boolean apply(final PaymentMethod paymentMethod) {
                                      return AdyenActivator.PLUGIN_NAME.equals(paymentMethod.getPluginName());
                                  }
                              }).getId();
    }

    private AdyenResponsesRecord previousAdyenResponseRecord(final UUID kbPaymentId, final String kbPaymentTransactionId, final CallContext context) {
        try {
            final AdyenResponsesRecord previousAuthorizationResponse = dao.getSuccessfulAuthorizationResponse(kbPaymentId, context.getTenantId());
            if (previousAuthorizationResponse != null && previousAuthorizationResponse.getKbPaymentTransactionId().equals(kbPaymentTransactionId)) {
                return previousAuthorizationResponse;
            }

            return null;
        } catch (final SQLException e) {
            logService.log(LogService.LOG_ERROR, "Failed to get previous AdyenResponsesRecord", e);
            return null;
        }
    }

    private String getMerchantAccount(final String countryCode, final Iterable<PluginProperty> properties, final TenantContext context) {
        return getMerchantAccount(countryCode, null, properties, context);
    }

    private String getMerchantAccount(final String countryCode, @Nullable final AdyenResponsesRecord adyenResponsesRecord, final Iterable<PluginProperty> properties, final TenantContext context) {
        final String paymentProcessorAccountId = PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_PROCESSOR_ACCOUNT_ID, properties);
        if (paymentProcessorAccountId != null) {
            return getConfigProperties(context)
                    .getMerchantAccountOfPaymentProcessorAccountId(paymentProcessorAccountId)
                    .orElseGet(new Supplier<String>() {
                        @Override
                        public String get() {
                            logger.debug("Cannot find a mapping for '{}', fallback to use it directly", paymentProcessorAccountId);
                            return paymentProcessorAccountId;
                        }
                    });
        }

        if (adyenResponsesRecord != null) {
            final String merchantAccountCode = getMerchantAccountFromRecord(adyenResponsesRecord);
            if (merchantAccountCode != null) {
                return merchantAccountCode;
            }
        }

        return getConfigProperties(context).getMerchantAccount(countryCode);
    }

    private String getMerchantAccountFromRecord(final AdyenResponsesRecord adyenResponsesRecord) {
        final Map additionalData = AdyenDao.fromAdditionalData(adyenResponsesRecord.getAdditionalData());
        final Object merchantAccountCode = additionalData.get(AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE);
        if (merchantAccountCode != null) {
            return merchantAccountCode.toString();
        }
        return null;
    }

    private AdyenConfigProperties getConfigProperties(final TenantContext context) {
        return adyenConfigPropertiesConfigurationHandler.getConfigurable(context.getTenantId());
    }

    public static String decode(final String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            return value;
        }
    }
}
