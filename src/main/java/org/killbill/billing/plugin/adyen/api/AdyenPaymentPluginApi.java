/*
 * Copyright 2014-2015 Groupon, Inc
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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.catalog.api.Currency;
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
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.OrderData;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.RecurringType;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.CreditCard;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Elv;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.OneClick;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Recurring;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.SepaDirectDebit;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.WebPaymentFrontend;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationHandler;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationService;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderHostedPaymentPagePort;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderPort;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.AdyenHostedPaymentPageConfigurationHandler;
import org.killbill.billing.plugin.adyen.core.KillbillAdyenNotificationHandler;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenPaymentMethods;
import org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenResponses;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentPluginApi;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.osgi.service.log.LogService;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import static org.killbill.billing.plugin.adyen.api.mapping.UserDataMappingService.toUserData;

public class AdyenPaymentPluginApi extends PluginPaymentPluginApi<AdyenResponsesRecord, AdyenResponses, AdyenPaymentMethodsRecord, AdyenPaymentMethods> {

    public static final String PROPERTY_HOLDER_NAME = "holderName";
    public static final String PROPERTY_SHIP_BEFORE_DATE = "shipBeforeDate";
    public static final String PROPERTY_INSTALLMENTS = "installments";
    public static final String PROPERTY_PAYMENT_PROVIDER_TYPE = "paymentProviderType";
    public static final String PROPERTY_PAYMENT_EXTERNAL_KEY = "paymentExternalKey";
    public static final String PROPERTY_SERVER_URL = "serverUrl";
    public static final String PROPERTY_RESULT_URL = "resultUrl";

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

    public static final String PROPERTY_CREATE_PENDING_PAYMENT = "createPendingPayment";

    public static final String PROPERTY_FROM_HPP = "fromHPP";
    public static final String PROPERTY_FROM_HPP_TRANSACTION_STATUS = "fromHPPTransactionStatus";

    public static final String PROPERTY_RECURRING_DETAIL_ID = "recurringDetailId";
    public static final String PROPERTY_RECURRING_TYPE = "recurringType";

    // 3-D Secure
    public static final String PROPERTY_PA_REQ = "PaReq";
    public static final String PROPERTY_PA_RES = "PaRes";
    public static final String PROPERTY_MD = "MD";
    public static final String PROPERTY_DCC_AMOUNT_VALUE = "dccAmount";
    public static final String PROPERTY_DCC_AMOUNT_CURRENCY = "dccCurrency";
    public static final String PROPERTY_DCC_SIGNATURE = "dccSignature";
    public static final String PROPERTY_ISSUER_URL = "issuerUrl";
    public static final String PROPERTY_TERM_URL = "TermUrl";
    public static final String PROPERTY_USER_AGENT = "userAgent";
    public static final String PROPERTY_ACCEPT_HEADER = "acceptHeader";
    public static final String PROPERTY_THREE_D_THRESHOLD = "threeDThreshold";

    public static final String PROPERTY_DD_HOLDER_NAME = "ddHolderName";
    public static final String PROPERTY_DD_ACCOUNT_NUMBER = "ddNumber";
    public static final String PROPERTY_DD_BANK_IDENTIFIER_CODE = "ddBic";
    public static final String PROPERTY_DD_BANKLEITZAHL = "ddBlz";

    /**
     * Cont auth disabled validation on adyens side (no cvc required). We practically tell them that the payment data is valid.
     * Should be given as "true" or "false".
     */
    public static final String PROPERTY_CONTINUOUS_AUTHENTICATION = "contAuth";

    private final AdyenConfigurationHandler adyenConfigurationHandler;
    private final AdyenHostedPaymentPageConfigurationHandler adyenHppConfigurationHandler;
    private final AdyenDao dao;
    private final AdyenNotificationService adyenNotificationService;

    public AdyenPaymentPluginApi(final AdyenConfigurationHandler adyenConfigurationHandler,
                                 final AdyenHostedPaymentPageConfigurationHandler adyenHppConfigurationHandler,
                                 final OSGIKillbillAPI killbillApi,
                                 final OSGIConfigPropertiesService osgiConfigPropertiesService,
                                 final OSGIKillbillLogService logService,
                                 final Clock clock,
                                 final AdyenDao dao) throws JAXBException {
        super(killbillApi, osgiConfigPropertiesService, logService, clock, dao);
        this.adyenConfigurationHandler = adyenConfigurationHandler;
        this.adyenHppConfigurationHandler = adyenHppConfigurationHandler;
        this.dao = dao;

        final AdyenNotificationHandler adyenNotificationHandler = new KillbillAdyenNotificationHandler(killbillApi, dao, clock);
        //noinspection RedundantTypeArguments
        this.adyenNotificationService = new AdyenNotificationService(ImmutableList.<AdyenNotificationHandler>of(adyenNotificationHandler));
    }

    @Override
    protected PaymentTransactionInfoPlugin buildPaymentTransactionInfoPlugin(final AdyenResponsesRecord adyenResponsesRecord) {
        return new AdyenPaymentTransactionInfoPlugin(adyenResponsesRecord);
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
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeInitialTransaction(TransactionType.AUTHORIZE,
                                        new TransactionExecutor<PurchaseResult>() {
                                            @Override
                                            public PurchaseResult execute(final Long amount, final PaymentData paymentData, final OrderData orderData, final UserData userData, final String termUrl, final SplitSettlementData splitSettlementData) {
                                                final AdyenPaymentServiceProviderPort adyenPort = adyenConfigurationHandler.getConfigurable(context.getTenantId());
                                                if (hasPreviousAdyenRespondseRecord(kbPaymentId, kbTransactionId.toString(), context)) {
                                                    final Map<String, String> requestParameterMap = PluginProperties.toStringMap(properties);
                                                    return adyenPort.authorize3DSecure(amount, paymentData, userData, requestParameterMap, splitSettlementData);
                                                } else {
                                                    return adyenPort.authorise(amount, paymentData, orderData, userData, termUrl, splitSettlementData);
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

    @Override
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.CAPTURE,
                                          new TransactionExecutor<PaymentModificationResponse>() {
                                              @Override
                                              public PaymentModificationResponse execute(@Nullable final Long transactionAmount, final PaymentProvider paymentProvider, final String pspReference, final SplitSettlementData splitSettlementData) {
                                                  final AdyenPaymentServiceProviderPort port = adyenConfigurationHandler.getConfigurable(context.getTenantId());
                                                  return port.capture(transactionAmount, paymentProvider, pspReference, splitSettlementData);
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

        final boolean fromHPP = Boolean.valueOf(PluginProperties.findPluginPropertyValue(PROPERTY_FROM_HPP, properties));
        if (!fromHPP) {
            // We already have a record for that payment transaction, update the response row with additional properties
            // (the API can be called for instance after the user is redirected back from the HPP to store the PSP reference)
            try {
                adyenResponsesRecord = dao.updateResponse(kbTransactionId, properties, context.getTenantId());
            } catch (final SQLException e) {
                throw new PaymentPluginApiException("HPP notification came through, but we encountered a database error", e);
            }
        } else {
            // We are either processing a notification (see KillbillAdyenNotificationHandler) or creating a PENDING payment for HPP (see buildFormDescriptor)
            final DateTime utcNow = clock.getUTCNow();
            try {
                //noinspection unchecked
                adyenResponsesRecord = dao.addAdyenResponse(kbAccountId, kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, PluginProperties.toMap(properties), utcNow, context.getTenantId());
            } catch (final SQLException e) {
                throw new PaymentPluginApiException("HPP notification came through, but we encountered a database error", e);
            }
        }

        return buildPaymentTransactionInfoPlugin(adyenResponsesRecord);
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.VOID,
                                          new TransactionExecutor<PaymentModificationResponse>() {
                                              @Override
                                              public PaymentModificationResponse execute(@Nullable final Long transactionAmount, final PaymentProvider paymentProvider, final String pspReference, final SplitSettlementData splitSettlementData) {
                                                  final AdyenPaymentServiceProviderPort port = adyenConfigurationHandler.getConfigurable(context.getTenantId());
                                                  return port.cancel(paymentProvider, pspReference, splitSettlementData);
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
        throw new PaymentPluginApiException(null, "CREDIT: unsupported operation");
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.REFUND,
                                          new TransactionExecutor<PaymentModificationResponse>() {
                                              @Override
                                              public PaymentModificationResponse execute(@Nullable final Long transactionAmount, final PaymentProvider paymentProvider, final String pspReference, final SplitSettlementData splitSettlementData) {
                                                  final AdyenPaymentServiceProviderPort providerPort = adyenConfigurationHandler.getConfigurable(context.getTenantId());
                                                  return providerPort.refund(transactionAmount, paymentProvider, pspReference, splitSettlementData);
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

        final String amountString = PluginProperties.findPluginPropertyValue(PROPERTY_AMOUNT, mergedProperties);
        Preconditions.checkState(!Strings.isNullOrEmpty(amountString), "amount not specified");
        final BigDecimal amountBD = new BigDecimal(amountString);
        final long amount = amountBD.longValue();

        final Account account = getAccount(kbAccountId, context);

        final PaymentData paymentData = buildPaymentData(account, mergedProperties, context);
        final OrderData orderData = buildOrderData(account, mergedProperties);
        final UserData userData = toUserData(account, mergedProperties);

        final boolean shouldCreatePendingPayment = Boolean.valueOf(PluginProperties.findPluginPropertyValue(PROPERTY_CREATE_PENDING_PAYMENT, mergedProperties));
        if (shouldCreatePendingPayment) {
            createPendingPayment(account, amountBD, paymentData, context);
        }

        final String serverUrl = PluginProperties.findPluginPropertyValue(PROPERTY_SERVER_URL, mergedProperties);
        Preconditions.checkState(!Strings.isNullOrEmpty(serverUrl), "serverUrl not specified");
        final String resultUrl = PluginProperties.findPluginPropertyValue(PROPERTY_RESULT_URL, mergedProperties);

        try {
            // Need to store on disk the mapping payment <-> user because Adyen's notification won't provide the latter
            //noinspection unchecked
            dao.addHppRequest(kbAccountId, paymentData.getPaymentTxnInternalRef(), PluginProperties.toMap(mergedProperties), clock.getUTCNow(), context.getTenantId());
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to store HPP request", e);
        }

        final AdyenPaymentServiceProviderHostedPaymentPagePort hostedPaymentPagePort = adyenHppConfigurationHandler.getConfigurable(context.getTenantId());

        final Map<String, String> formParameter;
        try {
            formParameter = hostedPaymentPagePort.getFormParameter(amount, paymentData, orderData, userData, serverUrl, resultUrl);
        } catch (final SignatureGenerationException e) {
            throw new PaymentPluginApiException("Unable to generate signature", e);
        }

        final String formUrl = hostedPaymentPagePort.getFormUrl(paymentData);

        try {
            return new AdyenHostedPaymentPageFormDescriptor(kbAccountId, formUrl, formParameter);
        } catch (final URISyntaxException e) {
            throw new PaymentPluginApiException("Unable to generate valid HPP url", e);
        }
    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final String notificationResponse = adyenNotificationService.handleNotifications(notification);
        return new AdyenGatewayNotification(notificationResponse);
    }

    private abstract static class TransactionExecutor<T> {

        public T execute(final Long amount, final PaymentData paymentData, final OrderData orderData, final UserData userData, final String termUrl, final SplitSettlementData splitSettlementData) {
            throw new UnsupportedOperationException();
        }

        public T execute(@Nullable final Long transactionAmount, final PaymentProvider paymentProvider, final String pspReference, final SplitSettlementData splitSettlementData) {
            throw new UnsupportedOperationException();
        }
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

        final Long transactionAmount = (amount == null ? null : amount.longValue());
        final PaymentData paymentData = buildPaymentData(account, kbPaymentId, kbTransactionId, kbPaymentMethodId, currency, properties, context);
        final OrderData orderData = buildOrderData(account, properties);
        final UserData userData = toUserData(account, properties);
        final String termUrl = PluginProperties.findPluginPropertyValue(PROPERTY_TERM_URL, properties);
        final SplitSettlementData splitSettlementData = null;
        final DateTime utcNow = clock.getUTCNow();

        final PurchaseResult response = transactionExecutor.execute(transactionAmount, paymentData, orderData, userData, termUrl, splitSettlementData);
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

        final Long transactionAmount = (amount == null ? null : amount.longValue());
        final PaymentProvider paymentProvider = buildPaymentProvider(account, kbPaymentMethodId, currency, properties, context);
        final SplitSettlementData splitSettlementData = null;

        final String pspReference;
        try {
            final AdyenResponsesRecord previousResponse = dao.getSuccessfulAuthorizationResponse(kbPaymentId, context.getTenantId());
            if (previousResponse == null) {
                throw new PaymentPluginApiException(null, "Unable to retrieve previous payment response for kbTransactionId " + kbTransactionId);
            }
            pspReference = previousResponse.getPspReference();
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Unable to retrieve previous payment response for kbTransactionId " + kbTransactionId, e);
        }

        final DateTime utcNow = clock.getUTCNow();

        final PaymentModificationResponse response;
        response = transactionExecutor.execute(transactionAmount, paymentProvider, pspReference, splitSettlementData);
        if (!response.isTechnicallySuccessful()) {
            return new AdyenPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, transactionType, amount, currency, Optional.<PaymentServiceProviderResult>absent(), utcNow, response);
        }

        try {
            dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, transactionType, amount, currency, response, utcNow, context.getTenantId());
            return new AdyenPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, transactionType, amount, currency, Optional.of(PaymentServiceProviderResult.RECEIVED), utcNow, response);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Payment went through, but we encountered a database error. Payment details: " + (response.toString()), e);
        }
    }

    // For API
    private PaymentData buildPaymentData(final AccountData account, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Currency currency, final Iterable<PluginProperty> properties, final TenantContext context) {
        final PaymentData<PaymentInfo> paymentData = new PaymentData<PaymentInfo>();

        Payment payment = null;
        PaymentTransaction paymentTransaction = null;
        try {
            payment = killbillAPI.getPaymentApi().getPayment(kbPaymentId, false, properties, context);
            //noinspection RedundantTypeArguments
            paymentTransaction = Iterables.<PaymentTransaction>find(payment.getTransactions(),
                                                new Predicate<PaymentTransaction>() {
                                                    @Override
                                                    public boolean apply(final PaymentTransaction input) {
                                                        return kbTransactionId.equals(input.getId());
                                                    }
                                                });
        } catch (final PaymentApiException e) {
            logService.log(LogService.LOG_WARNING, "Failed to retrieve payment " + kbPaymentId, e);
        }
        Preconditions.checkNotNull(payment);
        Preconditions.checkNotNull(paymentTransaction);

        paymentData.setPaymentId(kbPaymentId);
        paymentData.setPaymentInternalRef(payment.getExternalKey());
        paymentData.setPaymentTxnInternalRef(paymentTransaction.getExternalKey());
        paymentData.setPaymentInfo(buildPaymentInfo(account, kbPaymentMethodId, currency, properties, context));

        return paymentData;
    }

    // For HPP
    private PaymentData buildPaymentData(final AccountData account, final Iterable<PluginProperty> properties, final TenantContext context) {
        final PaymentData<WebPaymentFrontend> paymentData = new PaymentData<WebPaymentFrontend>();
        final String internalRef = PluginProperties.getValue(PROPERTY_PAYMENT_EXTERNAL_KEY, UUID.randomUUID().toString(), properties);
        paymentData.setPaymentInternalRef(internalRef);
        paymentData.setPaymentTxnInternalRef(internalRef);
        paymentData.setPaymentInfo(buildPaymentInfo(account, properties, context));
        return paymentData;
    }

    // For API
    private PaymentInfo buildPaymentInfo(final AccountData account, final UUID kbPaymentMethodId, final Currency currency, final Iterable<PluginProperty> properties, final TenantContext context) {
        final AdyenPaymentMethodsRecord paymentMethodsRecord = getAdyenPaymentMethodsRecord(kbPaymentMethodId, context);
        final AdyenPaymentMethodsRecord nonNullPaymentMethodsRecord = paymentMethodsRecord == null ? emptyRecord(kbPaymentMethodId) : paymentMethodsRecord;
        final Iterable<PluginProperty> additionalPropertiesFromRecord = buildPaymentMethodPlugin(nonNullPaymentMethodsRecord).getProperties();
        //noinspection unchecked
        final Iterable<PluginProperty> mergedProperties = PluginProperties.merge(additionalPropertiesFromRecord, properties);
        final PaymentProvider paymentProvider = buildPaymentProvider(account, nonNullPaymentMethodsRecord, currency, properties, context);

        final String paymentMethodsRecordCcType = nonNullPaymentMethodsRecord.getCcType();
        final String ccType = PluginProperties.getValue(PROPERTY_CC_TYPE, paymentMethodsRecordCcType, properties);
        final String recurringDetailId = PluginProperties.findPluginPropertyValue(PROPERTY_RECURRING_DETAIL_ID, mergedProperties);

        final PaymentInfo paymentInfo;
        if (recurringDetailId != null) {
            paymentInfo = buildRecurring(paymentMethodsRecord, paymentProvider, mergedProperties);
        } else if ("sepadirectdebit".equals(ccType)) {
            paymentInfo = buildSepaDirectDebit(nonNullPaymentMethodsRecord, paymentProvider, mergedProperties);
        } else if ("elv".equals(ccType)) {
            paymentInfo = buildElv(nonNullPaymentMethodsRecord, paymentProvider, mergedProperties);
        } else {
            paymentInfo = buildCreditCard(nonNullPaymentMethodsRecord, paymentProvider, mergedProperties);
        }

        paymentInfo.setContinuousAuthenticationEnabled(isContinuousAuthenticationEnabled(mergedProperties));

        return paymentInfo;
    }

    private boolean isContinuousAuthenticationEnabled(final Iterable<PluginProperty> mergedProperties) {
        final String contAuth = PluginProperties.findPluginPropertyValue(PROPERTY_CONTINUOUS_AUTHENTICATION, mergedProperties);
        return Boolean.parseBoolean(contAuth);
    }

    /**
     * There is the option not to use the adyen payment method table to retrieve payment data but to always provide
     * it as plugin properties. In this case an empty record (null object) could help.
     */
    private AdyenPaymentMethodsRecord emptyRecord(final UUID kbPaymentMethodId) {
        final AdyenPaymentMethodsRecord record = new AdyenPaymentMethodsRecord();
        record.setKbPaymentMethodId(kbPaymentMethodId.toString());
        return record;
    }

    private <C extends Card> C buildCard(final C card, final AdyenPaymentMethodsRecord paymentMethodsRecord, final Iterable<PluginProperty> properties) {
        // By convention, support the same keys as the Ruby plugins (https://github.com/killbill/killbill-plugin-framework-ruby/blob/master/lib/killbill/helpers/active_merchant/payment_plugin.rb)
        final String ccNumber = PluginProperties.getValue(PROPERTY_CC_NUMBER, paymentMethodsRecord.getCcNumber(), properties);
        card.setCcNumber(ccNumber);

        final String ccFirstName = PluginProperties.getValue(PROPERTY_CC_FIRST_NAME, paymentMethodsRecord.getCcFirstName(), properties);
        final String ccLastName = PluginProperties.getValue(PROPERTY_CC_LAST_NAME, paymentMethodsRecord.getCcLastName(), properties);
        card.setCcHolderName(holderName(ccFirstName, ccLastName));

        final String ccExpirationMonth = PluginProperties.getValue(PROPERTY_CC_EXPIRATION_MONTH, paymentMethodsRecord.getCcExpMonth(), properties);
        if (ccExpirationMonth != null) {
            card.setValidUntilMonth(Integer.valueOf(ccExpirationMonth));
        }

        final String ccExpirationYear = PluginProperties.getValue(PROPERTY_CC_EXPIRATION_YEAR, paymentMethodsRecord.getCcExpYear(), properties);
        if (ccExpirationYear != null) {
            card.setValidUntilYear(Integer.valueOf(ccExpirationYear));
        }

        final String ccVerificationValue = PluginProperties.getValue(PROPERTY_CC_VERIFICATION_VALUE, paymentMethodsRecord.getCcVerificationValue(), properties);
        if (ccVerificationValue != null) {
            card.setCcSecCode(ccVerificationValue);
        }

        final String ccUserAgent = PluginProperties.findPluginPropertyValue(PROPERTY_USER_AGENT, properties);
        final String ccAcceptHeader = PluginProperties.findPluginPropertyValue(PROPERTY_ACCEPT_HEADER, properties);
        if (ccUserAgent != null && ccAcceptHeader != null) {
            card.setUserAgent(ccUserAgent);
            card.setAcceptHeader(ccAcceptHeader);
        }

        final String md = PluginProperties.findPluginPropertyValue(PROPERTY_MD, properties);
        if (md != null ) {
            // TODO: decode might need to be done more general
            card.setMd(decode(md));
        }
        final String paRes = PluginProperties.findPluginPropertyValue(PROPERTY_PA_RES, properties);
        if (paRes != null) {
            // TODO: decode might need to be done more general
            card.setPaRes(decode(paRes));
        }
        return card;
    }

    /**
     * TODO: decode might need to be done more general.
     * @param value to be decoded
     * @return decoded value
     */
    private String decode(final String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private CreditCard buildCreditCard(final AdyenPaymentMethodsRecord paymentMethodsRecord, final PaymentProvider paymentProvider, final Iterable<PluginProperty> properties) {
        final CreditCard creditCard = buildCard(new CreditCard(paymentProvider), paymentMethodsRecord, properties);

        final String installments = PluginProperties.findPluginPropertyValue(PROPERTY_INSTALLMENTS, properties);
        if (installments != null ) {
            creditCard.setInstallments(Integer.valueOf(installments));
        }

        return creditCard;
    }

    private SepaDirectDebit buildSepaDirectDebit(final AdyenPaymentMethodsRecord paymentMethodsRecord, final PaymentProvider paymentProvider, final Iterable<PluginProperty> properties) {
        final SepaDirectDebit sepaDirectDebit = new SepaDirectDebit(paymentProvider);

        final String ddAccountNumber = PluginProperties.getValue(PROPERTY_DD_ACCOUNT_NUMBER, paymentMethodsRecord.getCcNumber(), properties);
        sepaDirectDebit.setIban(ddAccountNumber);

        final String paymentMethodHolderName = holderName(paymentMethodsRecord.getCcFirstName(), paymentMethodsRecord.getCcLastName());
        final String ddHolderName = PluginProperties.getValue(PROPERTY_DD_HOLDER_NAME, paymentMethodHolderName, properties);
        sepaDirectDebit.setSepaAccountHolder(ddHolderName);

        final String paymentMethodBic = PluginProperties.findPluginPropertyValue(PROPERTY_DD_BANK_IDENTIFIER_CODE, properties);
        final String ddBic = PluginProperties.getValue(PROPERTY_DD_BANK_IDENTIFIER_CODE, paymentMethodBic, properties);
        sepaDirectDebit.setBic(ddBic);

        final String countryCode = MoreObjects.firstNonNull(paymentMethodsRecord.getCountry(), paymentProvider.getCountryIsoCode());
        sepaDirectDebit.setCountryCode(countryCode);

        return sepaDirectDebit;
    }

    private Elv buildElv(final AdyenPaymentMethodsRecord paymentMethodsRecord, final PaymentProvider paymentProvider, final Iterable<PluginProperty> properties) {
        final Elv elv = new Elv(paymentProvider);

        final String ddAccountNumber = PluginProperties.getValue(PROPERTY_DD_ACCOUNT_NUMBER, paymentMethodsRecord.getCcNumber(), properties);
        elv.setElvKontoNummer(ddAccountNumber);

        final String paymentMethodHolderName = holderName(paymentMethodsRecord.getCcFirstName(), paymentMethodsRecord.getCcLastName());
        final String ddHolderName = PluginProperties.getValue(PROPERTY_DD_HOLDER_NAME, paymentMethodHolderName, properties);
        elv.setElvAccountHolder(ddHolderName);

        final String paymentMethodBlz = PluginProperties.findPluginPropertyValue(PROPERTY_DD_BANKLEITZAHL, properties);
        final String ddBlz = PluginProperties.getValue(PROPERTY_DD_BANKLEITZAHL, paymentMethodBlz, properties);
        elv.setElvBlz(ddBlz);

        return elv;
    }

    private Recurring buildRecurring(final AdyenPaymentMethodsRecord paymentMethodsRecord, final PaymentProvider paymentProvider, final Iterable<PluginProperty> properties) {
        final String recurringDetailId = PluginProperties.findPluginPropertyValue(PROPERTY_RECURRING_DETAIL_ID, properties);
        final String ccVerificationValue = PluginProperties.getValue(PROPERTY_CC_VERIFICATION_VALUE, paymentMethodsRecord.getCcVerificationValue(), properties);

        if (RecurringType.ONECLICK.equals(paymentProvider.getRecurringType())) {
            final OneClick oneClick = new OneClick(paymentProvider);
            oneClick.setRecurringDetailId(recurringDetailId);
            oneClick.setCcSecCode(ccVerificationValue);
            return oneClick;
        } else {
            final Recurring recurring = new Recurring(paymentProvider);
            recurring.setRecurringDetailId(recurringDetailId);
            return recurring;
        }
    }

    private static String holderName(final String firstName, final String lastName) {
        return String.format("%s%s", firstName == null ? "" : firstName + " ", lastName);
    }

    // For HPP
    private WebPaymentFrontend buildPaymentInfo(final AccountData account, final Iterable<PluginProperty> properties, final TenantContext context) {
        final PaymentProvider paymentProvider = buildPaymentProvider(account, (UUID) null, null, properties, context);
        return new WebPaymentFrontend(paymentProvider);
    }

    private AdyenPaymentMethodsRecord getAdyenPaymentMethodsRecord(final UUID kbPaymentMethodId, final TenantContext context) {
        AdyenPaymentMethodsRecord paymentMethodsRecord = null;
        try {
            paymentMethodsRecord = dao.getPaymentMethod(kbPaymentMethodId, context.getTenantId());
        } catch (final SQLException e) {
            logService.log(LogService.LOG_WARNING, "Failed to retrieve payment method " + kbPaymentMethodId, e);
        }
        return paymentMethodsRecord;
    }

    private PaymentProvider buildPaymentProvider(final AccountData account, @Nullable final UUID kbPaymentMethodId, @Nullable final Currency currency, final Iterable<PluginProperty> properties, final TenantContext context) {
        final AdyenPaymentMethodsRecord paymentMethodsRecord = kbPaymentMethodId == null ? null : getAdyenPaymentMethodsRecord(kbPaymentMethodId, context);
        return buildPaymentProvider(account, paymentMethodsRecord, currency, properties, context);
    }

    private PaymentProvider buildPaymentProvider(final AccountData account, @Nullable final AdyenPaymentMethodsRecord paymentMethodsRecord, @Nullable final Currency currency, final Iterable<PluginProperty> properties, final TenantContext context) {
        final String pluginPropertyCurrency = PluginProperties.findPluginPropertyValue(PROPERTY_CURRENCY, properties);
        final String paymentProviderCurrency = pluginPropertyCurrency == null ? (currency == null ? null : currency.name()) : pluginPropertyCurrency;
        final String pluginPropertyCountry = PluginProperties.findPluginPropertyValue(PROPERTY_COUNTRY, properties);
        final String paymentProviderCountryIsoCode = pluginPropertyCountry == null ? account.getCountry() : pluginPropertyCountry;
        final String threeDThreshold = PluginProperties.findPluginPropertyValue(PROPERTY_THREE_D_THRESHOLD, properties);

        final String pluginPropertyPaymentProviderType = PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_PROVIDER_TYPE, properties);
        final String recurringDetailId = PluginProperties.findPluginPropertyValue(PROPERTY_RECURRING_DETAIL_ID, properties);
        final String pluginPropertyPaymentRecurringType = PluginProperties.findPluginPropertyValue(PROPERTY_RECURRING_TYPE, properties);

        final PaymentType paymentProviderPaymentType;
        if (recurringDetailId != null) {
            paymentProviderPaymentType = PaymentType.EMPTY;
        } else if (pluginPropertyPaymentProviderType != null) {
            paymentProviderPaymentType = PaymentType.getByName(pluginPropertyPaymentProviderType);
        } else {
            final String pluginPropertyCCType = PluginProperties.findPluginPropertyValue(PROPERTY_CC_TYPE, properties);
            final String paymentMethodCCType = paymentMethodsRecord == null || paymentMethodsRecord.getCcType() == null ? null : paymentMethodsRecord.getCcType();
            paymentProviderPaymentType = pluginPropertyCCType == null ? (paymentMethodCCType == null ? PaymentType.CREDITCARD : PaymentType.getByName(paymentMethodCCType)) : PaymentType.getByName(pluginPropertyCCType);
        }

        final RecurringType paymentProviderRecurringType;
        if (pluginPropertyPaymentRecurringType != null) {
            paymentProviderRecurringType = RecurringType.valueOf(pluginPropertyPaymentRecurringType);
        } else {
            paymentProviderRecurringType = null;
        }

        // A bit of a hack - it would be nice to be able to isolate AdyenConfigProperties
        final AdyenConfigProperties adyenConfigProperties = adyenHppConfigurationHandler.getConfigurable(context.getTenantId()).getAdyenConfigProperties();

        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        if (paymentProviderCurrency != null) {
            paymentProvider.setCurrency(java.util.Currency.getInstance(paymentProviderCurrency));
        }
        paymentProvider.setCountryIsoCode(paymentProviderCountryIsoCode);
        paymentProvider.setPaymentType(paymentProviderPaymentType);
        paymentProvider.setRecurringType(paymentProviderRecurringType);
        paymentProvider.setThreeDThreshold(parseThreeDThreshold(threeDThreshold));

        return paymentProvider;
    }

    private Long parseThreeDThreshold(final String threeDThresholdStr) {
        if (Strings.isNullOrEmpty(threeDThresholdStr)) {
            return null;
        } else {
            try {
                return Long.valueOf(threeDThresholdStr);
            } catch (final NumberFormatException e) {
                logService.log(LogService.LOG_ERROR, "Malformed ThreeD Threshold: " + threeDThresholdStr, e);
                return null;
            }
        }
    }

    private OrderData buildOrderData(@Nullable final AccountData account, final Iterable<PluginProperty> properties) {
        final OrderData orderData = new OrderData();

        final String accountName = account == null ? null : account.getName();
        orderData.setHolderName(PluginProperties.getValue(PROPERTY_HOLDER_NAME, accountName, properties));

        final String propertyShipBeforeDate = PluginProperties.findPluginPropertyValue(PROPERTY_SHIP_BEFORE_DATE, properties);
        // Mandatory for HPP
        orderData.setShipBeforeDate(propertyShipBeforeDate == null ? clock.getUTCNow().plusHours(1) : new DateTime(propertyShipBeforeDate));

        return orderData;
    }

    private void createPendingPayment(final Account account, final BigDecimal amount, final PaymentData paymentData, final CallContext context) throws PaymentPluginApiException {
        final UUID kbPaymentId = null;
        final Currency currency = Currency.valueOf(paymentData.getPaymentInfo().getPaymentProvider().getCurrency().toString());
        final String paymentExternalKey = paymentData.getPaymentTxnInternalRef();
        //noinspection UnnecessaryLocalVariable
        final String paymentTransactionExternalKey = paymentExternalKey;
        final ImmutableMap<String, Object> purchasePropertiesMap = ImmutableMap.<String, Object>of(AdyenPaymentPluginApi.PROPERTY_FROM_HPP, true,
                                                                                                   AdyenPaymentPluginApi.PROPERTY_FROM_HPP_TRANSACTION_STATUS, PaymentPluginStatus.PENDING.toString());
        final Iterable<PluginProperty> purchaseProperties = PluginProperties.buildPluginProperties(purchasePropertiesMap);

        try {
            final UUID kbPaymentMethodId = getAdyenKbPaymentMethodId(account.getId(), context);
            killbillAPI.getPaymentApi().createPurchase(account,
                                                       kbPaymentMethodId,
                                                       kbPaymentId,
                                                       amount,
                                                       currency,
                                                       paymentExternalKey,
                                                       paymentTransactionExternalKey,
                                                       purchaseProperties,
                                                       context);
        } catch (final PaymentApiException e) {
            throw new PaymentPluginApiException("Failed to record purchase", e);
        }
    }

    // Could be shared (see KillbillAdyenNotificationHandler)
    private UUID getAdyenKbPaymentMethodId(final UUID kbAccountId, final TenantContext context) throws PaymentApiException {
        //noinspection RedundantTypeArguments
        return Iterables.<PaymentMethod>find(killbillAPI.getPaymentApi().getAccountPaymentMethods(kbAccountId, false, ImmutableList.<PluginProperty>of(), context),
                              new Predicate<PaymentMethod>() {
                                  @Override
                                  public boolean apply(final PaymentMethod paymentMethod) {
                                      return AdyenActivator.PLUGIN_NAME.equals(paymentMethod.getPluginName());
                                  }
                              }).getId();
    }

    private boolean hasPreviousAdyenRespondseRecord(final UUID kbPaymentId, final String kbPaymentTransactionId, final CallContext context) {
        try {
            final AdyenResponsesRecord previousAuthorizationResponse = dao.getSuccessfulAuthorizationResponse(kbPaymentId, context.getTenantId());
            return previousAuthorizationResponse != null && previousAuthorizationResponse.getKbPaymentTransactionId().equals(kbPaymentTransactionId);
        } catch (final SQLException e) {
            logService.log(LogService.LOG_ERROR, "Failed to get previous AdyenResponsesRecord", e);
            return false;
        }
    }
}
