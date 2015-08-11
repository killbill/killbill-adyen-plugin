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

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
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
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.CreditCard;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.WebPaymentFrontend;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationHandler;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationService;
import org.killbill.billing.plugin.adyen.client.payment.exception.ModificationFailedException;
import org.killbill.billing.plugin.adyen.client.payment.exception.SignatureGenerationException;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderHostedPaymentPagePort;
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
import org.killbill.clock.Clock;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.osgi.service.log.LogService;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class AdyenPaymentPluginApi extends PluginPaymentPluginApi<AdyenResponsesRecord, AdyenResponses, AdyenPaymentMethodsRecord, AdyenPaymentMethods> {

    public static final String PROPERTY_CUSTOMER_ID = "customerId";
    public static final String PROPERTY_CUSTOMER_LOCALE = "customerLocale";
    public static final String PROPERTY_EMAIL = "email";
    public static final String PROPERTY_FIRST_NAME = "firstName";
    public static final String PROPERTY_LAST_NAME = "lastName";
    public static final String PROPERTY_IP = "ip";
    public static final String PROPERTY_HOLDER_NAME = "holderName";
    public static final String PROPERTY_SHIP_BEFORE_DATE = "shipBeforeDate";
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

    public static final String PROPERTY_FROM_HPP = "fromHPP";

    // 3-D Secure
    public static final String PROPERTY_PA_REQ = "PaReq";
    public static final String PROPERTY_MD = "MD";
    public static final String PROPERTY_DCC_AMOUNT_VALUE = "dccAmount";
    public static final String PROPERTY_DCC_AMOUNT_CURRENCY = "dccCurrency";
    public static final String PROPERTY_DCC_SIGNATURE = "dccSignature";
    public static final String PROPERTY_ISSUER_URL = "issuerUrl";
    public static final String PROPERTY_TERM_URL = "TermUrl";

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

        final KillbillAdyenNotificationHandler adyenNotificationHandler = new KillbillAdyenNotificationHandler(killbillApi, dao, clock);
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
                                         new TransactionExecutor<PurchaseResult, RuntimeException>() {
                                             @Override
                                             public PurchaseResult execute(final Long amount, final PaymentData paymentData, final OrderData orderData, final UserData userData, final String termUrl, final SplitSettlementData splitSettlementData) {
                                                 return adyenConfigurationHandler.getConfigurable(context.getTenantId()).authorise(amount, paymentData, orderData, userData, termUrl, splitSettlementData);
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
                                          new TransactionExecutor<PaymentModificationResponse, ModificationFailedException>() {
                                              @Override
                                              public PaymentModificationResponse execute(@Nullable final Long transactionAmount, final PaymentProvider paymentProvider, final String pspReference, final SplitSettlementData splitSettlementData) throws ModificationFailedException {
                                                  return adyenConfigurationHandler.getConfigurable(context.getTenantId()).capture(transactionAmount, paymentProvider, pspReference, splitSettlementData);
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
        final boolean fromHPP = Boolean.valueOf(PluginProperties.findPluginPropertyValue(PROPERTY_FROM_HPP, properties));
        if (!fromHPP) {
            throw new PaymentPluginApiException(null, "PURCHASE: unsupported operation");
        } else {
            // We are processing a notification, see KillbillAdyenNotificationHandler
            final DateTime utcNow = clock.getUTCNow();
            try {
                dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, PluginProperties.toMap(properties), utcNow, context.getTenantId());
                return new AdyenPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, TransactionType.PURCHASE, amount, currency, utcNow, PaymentPluginStatus.PROCESSED);
            } catch (final SQLException e) {
                throw new PaymentPluginApiException("HPP notification came through, but we encountered a database error", e);
            }
        }
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return executeFollowUpTransaction(TransactionType.VOID,
                                          new TransactionExecutor<PaymentModificationResponse, ModificationFailedException>() {
                                              @Override
                                              public PaymentModificationResponse execute(@Nullable final Long transactionAmount, final PaymentProvider paymentProvider, final String pspReference, final SplitSettlementData splitSettlementData) throws ModificationFailedException {
                                                  return adyenConfigurationHandler.getConfigurable(context.getTenantId()).cancel(paymentProvider, pspReference, splitSettlementData);
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
                                          new TransactionExecutor<PaymentModificationResponse, ModificationFailedException>() {
                                              @Override
                                              public PaymentModificationResponse execute(@Nullable final Long transactionAmount, final PaymentProvider paymentProvider, final String pspReference, final SplitSettlementData splitSettlementData) throws ModificationFailedException {
                                                  return adyenConfigurationHandler.getConfigurable(context.getTenantId()).refund(transactionAmount, paymentProvider, pspReference, splitSettlementData);
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
        final Iterable<PluginProperty> mergedProperties = PluginProperties.merge(customFields, properties);

        final String amountString = PluginProperties.findPluginPropertyValue(PROPERTY_AMOUNT, mergedProperties);
        Preconditions.checkState(!Strings.isNullOrEmpty(amountString), "amount not specified");
        final long amount = new BigDecimal(amountString).longValue();

        final Account account = getAccount(kbAccountId, context);

        final PaymentData paymentData = buildPaymentData(account, mergedProperties, context);
        final OrderData orderData = buildOrderData(account, mergedProperties);
        final UserData userData = buildUserData(account, mergedProperties);

        final String serverUrl = PluginProperties.findPluginPropertyValue(PROPERTY_SERVER_URL, mergedProperties);
        Preconditions.checkState(!Strings.isNullOrEmpty(serverUrl), "serverUrl not specified");
        final String resultUrl = PluginProperties.findPluginPropertyValue(PROPERTY_RESULT_URL, mergedProperties);

        try {
            // Need to store on disk the mapping payment <-> user because Adyen's notification won't provide the latter
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

    private static abstract class TransactionExecutor<T, E extends Throwable> {

        public T execute(final Long amount, final PaymentData paymentData, final OrderData orderData, final UserData userData, final String termUrl, final SplitSettlementData splitSettlementData) throws E {
            throw new UnsupportedOperationException();
        }

        public T execute(@Nullable final Long transactionAmount, final PaymentProvider paymentProvider, final String pspReference, final SplitSettlementData splitSettlementData) throws E {
            throw new UnsupportedOperationException();
        }
    }

    private PaymentTransactionInfoPlugin executeInitialTransaction(final TransactionType transactionType,
                                                                   final TransactionExecutor<PurchaseResult, RuntimeException> transactionExecutor,
                                                                   final UUID kbAccountId,
                                                                   final UUID kbPaymentId,
                                                                   final UUID kbTransactionId,
                                                                   final UUID kbPaymentMethodId,
                                                                   final BigDecimal amount,
                                                                   final Currency currency,
                                                                   final Iterable<PluginProperty> properties,
                                                                   final CallContext context) throws PaymentPluginApiException {
        final Account account = getAccount(kbAccountId, context);

        final Long transactionAmount = (amount == null ? null : amount.longValue());
        final PaymentData paymentData = buildPaymentData(account, kbPaymentId, kbTransactionId, kbPaymentMethodId, currency, properties, context);
        final OrderData orderData = buildOrderData(account, properties);
        final UserData userData = buildUserData(account, properties);
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
                                                                    final TransactionExecutor<PaymentModificationResponse, ModificationFailedException> transactionExecutor,
                                                                    final UUID kbAccountId,
                                                                    final UUID kbPaymentId,
                                                                    final UUID kbTransactionId,
                                                                    final UUID kbPaymentMethodId,
                                                                    @Nullable final BigDecimal amount,
                                                                    @Nullable final Currency currency,
                                                                    final Iterable<PluginProperty> properties,
                                                                    final CallContext context) throws PaymentPluginApiException {
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
        try {
            response = transactionExecutor.execute(transactionAmount, paymentProvider, pspReference, splitSettlementData);
        } catch (final ModificationFailedException e) {
            return new AdyenPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, transactionType, amount, currency, PaymentServiceProviderResult.ERROR, utcNow, e);
        }

        try {
            dao.addResponse(kbAccountId, kbPaymentId, kbTransactionId, transactionType, amount, currency, response, utcNow, context.getTenantId());
            return new AdyenPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, transactionType, amount, currency, PaymentServiceProviderResult.RECEIVED, utcNow, response);
        } catch (final SQLException e) {
            throw new PaymentPluginApiException("Payment went through, but we encountered a database error. Payment details: " + (response == null ? null : response.toString()), e);
        }
    }

    // For API
    private PaymentData buildPaymentData(final Account account, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) {
        final PaymentData<PaymentInfo> paymentData = new PaymentData<PaymentInfo>();

        Payment payment = null;
        PaymentTransaction paymentTransaction = null;
        try {
            payment = killbillAPI.getPaymentApi().getPayment(kbPaymentId, false, properties, context);
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
    private PaymentData buildPaymentData(final Account account, final Iterable<PluginProperty> properties, final CallContext context) {
        final PaymentData<WebPaymentFrontend> paymentData = new PaymentData<WebPaymentFrontend>();
        final String internalRef = PluginProperties.getValue(PROPERTY_PAYMENT_EXTERNAL_KEY, UUID.randomUUID().toString(), properties);
        paymentData.setPaymentInternalRef(internalRef);
        paymentData.setPaymentTxnInternalRef(internalRef);
        paymentData.setPaymentInfo(buildPaymentInfo(account, properties, context));
        return paymentData;
    }

    // For API
    // TODO Generify
    private PaymentInfo buildPaymentInfo(final Account account, final UUID kbPaymentMethodId, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) {
        final AdyenPaymentMethodsRecord paymentMethodsRecord = getAdyenPaymentMethodsRecord(kbPaymentMethodId, context);
        final PaymentProvider paymentProvider = buildPaymentProvider(account, paymentMethodsRecord, currency, properties, context);

        // By convention, support the same keys as the Ruby plugins (https://github.com/killbill/killbill-plugin-framework-ruby/blob/master/lib/killbill/helpers/active_merchant/payment_plugin.rb)
        final String pluginPropertyCcNumber = PluginProperties.findPluginPropertyValue(PROPERTY_CC_NUMBER, properties);
        final String paymentMethodCcNumber = paymentMethodsRecord == null ? null : paymentMethodsRecord.getCcNumber();
        final String ccNumber = pluginPropertyCcNumber == null ? paymentMethodCcNumber : pluginPropertyCcNumber;
        if (ccNumber != null) {
            final String paymentMethodExpirationMonth = paymentMethodsRecord == null ? null : paymentMethodsRecord.getCcExpMonth();
            final String ccExpirationMonth = PluginProperties.getValue(PROPERTY_CC_EXPIRATION_MONTH, paymentMethodExpirationMonth, properties);

            final String paymentMethodExpirationYear = paymentMethodsRecord == null ? null : paymentMethodsRecord.getCcExpYear();
            final String ccExpirationYear = PluginProperties.getValue(PROPERTY_CC_EXPIRATION_YEAR, paymentMethodExpirationYear, properties);

            final String paymentMethodVerificationValue = paymentMethodsRecord == null ? null : paymentMethodsRecord.getCcVerificationValue();
            final String ccVerificationValue = PluginProperties.getValue(PROPERTY_CC_VERIFICATION_VALUE, paymentMethodVerificationValue, properties);

            final String paymentMethodCcFirstName = paymentMethodsRecord == null ? null : paymentMethodsRecord.getCcFirstName();
            final String ccFirstName = PluginProperties.getValue(PROPERTY_CC_FIRST_NAME, paymentMethodCcFirstName, properties);

            final String paymentMethodCcLastName = paymentMethodsRecord == null ? null : paymentMethodsRecord.getCcLastName();
            final String ccLastName = PluginProperties.getValue(PROPERTY_CC_LAST_NAME, paymentMethodCcLastName, properties);

            final CreditCard paymentInfo = new CreditCard(paymentProvider);
            paymentInfo.setCcHolderName(String.format("%s%s%s", ccFirstName, ccFirstName == null ? "" : " ", ccLastName));
            paymentInfo.setCcNumber(ccNumber);
            if (ccExpirationMonth != null) {
                paymentInfo.setValidUntilMonth(Integer.valueOf(ccExpirationMonth));
            }
            if (ccExpirationYear != null) {
                paymentInfo.setValidUntilYear(Integer.valueOf(ccExpirationYear));
            }
            if (ccVerificationValue != null) {
                paymentInfo.setCcSecCode(ccVerificationValue);
            }

            return paymentInfo;
        }
        return null;
    }

    // For HPP
    private WebPaymentFrontend buildPaymentInfo(final Account account, final Iterable<PluginProperty> properties, final CallContext context) {
        final PaymentProvider paymentProvider = buildPaymentProvider(account, (UUID) null, null, properties, context);
        return new WebPaymentFrontend(paymentProvider);
    }

    private AdyenPaymentMethodsRecord getAdyenPaymentMethodsRecord(final UUID kbPaymentMethodId, final CallContext context) {
        AdyenPaymentMethodsRecord paymentMethodsRecord = null;
        try {
            paymentMethodsRecord = dao.getPaymentMethod(kbPaymentMethodId, context.getTenantId());
        } catch (final SQLException e) {
            logService.log(LogService.LOG_WARNING, "Failed to retrieve payment method " + kbPaymentMethodId, e);
        }
        return paymentMethodsRecord;
    }

    private PaymentProvider buildPaymentProvider(final Account account, @Nullable final UUID kbPaymentMethodId, @Nullable final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) {
        final AdyenPaymentMethodsRecord paymentMethodsRecord = kbPaymentMethodId == null ? null : getAdyenPaymentMethodsRecord(kbPaymentMethodId, context);
        return buildPaymentProvider(account, paymentMethodsRecord, currency, properties, context);
    }

    private PaymentProvider buildPaymentProvider(final Account account, @Nullable final AdyenPaymentMethodsRecord paymentMethodsRecord, @Nullable final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) {
        final String pluginPropertyCurrency = PluginProperties.findPluginPropertyValue(PROPERTY_CURRENCY, properties);
        final String paymentProviderCurrency = pluginPropertyCurrency == null ? (currency == null ? null : currency.name()) : pluginPropertyCurrency;
        final String pluginPropertyCountry = PluginProperties.findPluginPropertyValue(PROPERTY_COUNTRY, properties);
        final String paymentProviderCountryIsoCode = pluginPropertyCountry == null ? account.getCountry() : pluginPropertyCountry;

        final PaymentType paymentProviderPaymentType;
        final String pluginPropertyPaymentProviderType = PluginProperties.findPluginPropertyValue(PROPERTY_PAYMENT_PROVIDER_TYPE, properties);
        if (pluginPropertyPaymentProviderType != null) {
            paymentProviderPaymentType = PaymentType.valueOf(pluginPropertyPaymentProviderType);
        } else {
            final String pluginPropertyCCType = PluginProperties.findPluginPropertyValue(PROPERTY_CC_TYPE, properties);
            final String paymentMethodCCType = paymentMethodsRecord == null || paymentMethodsRecord.getCcType() == null ? null : paymentMethodsRecord.getCcType();
            paymentProviderPaymentType = pluginPropertyCCType == null ? (paymentMethodCCType == null ? PaymentType.CREDITCARD : PaymentType.valueOf(paymentMethodCCType)) : PaymentType.valueOf(pluginPropertyCCType.toUpperCase());
        }

        // A bit of a hack - it would be nice to be able to isolate AdyenConfigProperties
        final AdyenConfigProperties adyenConfigProperties = adyenHppConfigurationHandler.getConfigurable(context.getTenantId()).getAdyenConfigProperties();

        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        if (paymentProviderCurrency != null) {
            paymentProvider.setCurrency(java.util.Currency.getInstance(paymentProviderCurrency));
        }
        paymentProvider.setCountryIsoCode(paymentProviderCountryIsoCode);
        paymentProvider.setPaymentType(paymentProviderPaymentType);

        return paymentProvider;
    }

    private UserData buildUserData(@Nullable final Account account, final Iterable<PluginProperty> properties) {
        final UserData userData = new UserData();

        final String accountCustomerId = account == null ? null : (account.getExternalKey() == null ? account.getId().toString() : account.getExternalKey());
        userData.setCustomerId(PluginProperties.getValue(PROPERTY_CUSTOMER_ID, accountCustomerId, properties));

        final String propertyLocaleString = PluginProperties.findPluginPropertyValue(PROPERTY_CUSTOMER_LOCALE, properties);
        final Locale propertyCustomerLocale = propertyLocaleString == null ? null : Locale.forLanguageTag(propertyLocaleString);
        final Locale accountLocale = account == null || account.getLocale() == null ? null : new Locale(account.getLocale());
        userData.setCustomerLocale(propertyCustomerLocale == null ? accountLocale : propertyCustomerLocale);

        final String accountEmail = account == null ? null : account.getEmail();
        userData.setEmail(PluginProperties.getValue(PROPERTY_EMAIL, accountEmail, properties));

        final String accountFirstName = account == null || account.getName() == null ? null : account.getName().substring(0, Objects.firstNonNull(account.getFirstNameLength(), account.getName().length()));
        userData.setFirstName(PluginProperties.getValue(PROPERTY_FIRST_NAME, accountFirstName, properties));

        final String accountLastName = account == null || account.getName() == null ? null : account.getName().substring(Objects.firstNonNull(account.getFirstNameLength(), account.getName().length()), account.getName().length());
        userData.setLastName(PluginProperties.getValue(PROPERTY_LAST_NAME, accountLastName, properties));

        userData.setIP(PluginProperties.findPluginPropertyValue(PROPERTY_IP, properties));

        return userData;
    }

    private OrderData buildOrderData(@Nullable final Account account, final Iterable<PluginProperty> properties) {
        final OrderData orderData = new OrderData();

        final String accountName = account == null ? null : account.getName();
        orderData.setHolderName(PluginProperties.getValue(PROPERTY_HOLDER_NAME, accountName, properties));

        final String propertyShipBeforeDate = PluginProperties.findPluginPropertyValue(PROPERTY_SHIP_BEFORE_DATE, properties);
        // Mandatory for HPP
        orderData.setShipBeforeDate(propertyShipBeforeDate == null ? clock.getUTCNow().plusHours(1) : new DateTime(propertyShipBeforeDate));

        return orderData;
    }
}
