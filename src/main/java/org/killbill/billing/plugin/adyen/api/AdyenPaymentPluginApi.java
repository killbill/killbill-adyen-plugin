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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import org.killbill.adyen.common.Amount;
import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.payment.Card;
import org.killbill.adyen.payment.PaymentRequest;
import org.killbill.adyen.payment.PaymentRequest3D;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.adyen.payment.ServiceException;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.client.AdyenRequestSender;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPluginGatewayResponsesRecord;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.killbill.clock.Clock;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.osgi.service.log.LogService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdyenPaymentPluginApi implements PaymentPluginApi {

    private final static String PROP_ISSUER_URL = "IssuerUrl";
    private final static String PROP_PA_REQ = "PaReq";
    private final static String PROP_MD = "MD";
    private final static String PROP_ACCEPT_HEADER = "AcceptHeader";
    private final static String PROP_USER_AGENT = "UserAgent";
    private final static String PROP_PA_RES = "PaRes";


    public static final String ERROR_3DS_AUTHORIZATION_ = "3ds completion authorization failed";
    public static final String ERROR_AUTHORIZATION_FAILED = "authorization failed";
    public static final String ERROR_ACCOUNT_NOT_FOUND = "no such account";
    public static final String ERROR_SQL_READ = "failed to access plugin database entries";
    public static final String ERROR_UNKNOWN = "unknown";


    private final AdyenRequestSender adyenClient;
    private final OSGIKillbillAPI killbillApi;
    private final LogService logService;
    private final AdyenDao dao;
    private final Clock clock;
    private final String merchantAccount;


    public AdyenPaymentPluginApi(final OSGIKillbillAPI killbillApi, AdyenDao dao, final Clock clock, final LogService logService, final AdyenRequestSender adyenClient, final String merchantAccount) {
        this.killbillApi = killbillApi;
        this.adyenClient = adyenClient;
        this.dao = dao;
        this.logService = logService;
        this.clock = clock;
        this.merchantAccount = merchantAccount;
    }


    @Override
    public PaymentTransactionInfoPlugin authorizePayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {

        try {
            final Account account = killbillApi.getAccountUserApi().getAccountById(kbAccountId, context);


            final Map<String, String> properties3DS;

            final List<AdyenPluginGatewayResponsesRecord> existingEntries = dao.getEntriesForPaymentId(kbPaymentId);
            final AdyenPluginGatewayResponsesRecord initialAuthorization = Iterables.tryFind(existingEntries, new Predicate<AdyenPluginGatewayResponsesRecord>() {
                @Override
                public boolean apply(AdyenPluginGatewayResponsesRecord input) {
                    return "RedirectShopper".equals(input.getResultCode()) &&
                            "AUTHORIZE".equals(input.getTransactionType());
                }
            }).orNull();

            final PaymentResult paymentResult;
            if (initialAuthorization == null) {
                properties3DS = toMap(properties, new Predicate<PluginProperty>() {
                    @Override
                    public boolean apply(PluginProperty input) {
                        return input.getKey().equals(PROP_ACCEPT_HEADER) ||
                                input.getKey().equals(PROP_USER_AGENT);
                    }
                });


                final PaymentRequest request = createAuthorizeRequest(kbPaymentId, amount, account, properties3DS);
                paymentResult = adyenClient.authorize(toCountryCode(account.getCountry()), request);

                if (paymentResult.getMd() != null && paymentResult.getPaRequest() != null) {
                    StringBuffer tmp = new StringBuffer("3DS parameters:\n\n");
                    tmp.append("MD: ");
                    tmp.append(paymentResult.getMd());
                    tmp.append("\n\n");

                    tmp.append("PaReq: ");
                    tmp.append(paymentResult.getPaRequest());
                    tmp.append("\n\n");

                    logService.log(LogService.LOG_DEBUG, tmp.toString());
                }
            } else {

                properties3DS = toMap(properties, new Predicate<PluginProperty>() {
                    @Override
                    public boolean apply(PluginProperty input) {
                        return input.getKey().equals(PROP_ACCEPT_HEADER) ||
                                input.getKey().equals(PROP_USER_AGENT) ||
                                input.getKey().equals(PROP_MD) ||
                                input.getKey().equals(PROP_PA_RES);
                    }
                });

                if (properties3DS.get(PROP_MD) == null ||
                        !properties3DS.get(PROP_MD).equals(initialAuthorization.getMd()) ||
                        properties3DS.get(PROP_PA_RES) == null) {
                    throw new PaymentPluginApiException(ERROR_3DS_AUTHORIZATION_, "Failed to auth account " + kbAccountId + ", paymentId = " + kbPaymentId + " for amount " + amount + " : wrong input parameters");
                }
                final PaymentRequest3D request = createAuthorize3DRequest(kbPaymentId, properties3DS);
                paymentResult = adyenClient.authorize3D(toCountryCode(account.getCountry()), request);
            }
            try {
                dao.insertPaymentResult(kbAccountId, kbPaymentId, TransactionType.AUTHORIZE.toString(), paymentResult);
            } catch (SQLException e) {
                logService.log(LogService.LOG_ERROR, "Failed to insert authorized payment for account " + kbAccountId + ", payment " + kbPaymentId + ", authorized code = " + paymentResult.getAuthCode());
            }
            return toPaymentInfoPlugin(paymentResult, kbPaymentId, kbTransactionId, amount, clock.getUTCNow());
        } catch (AccountApiException e) {
            throw new PaymentPluginApiException(ERROR_ACCOUNT_NOT_FOUND, "Failed to auth account " + kbAccountId + ", paymentId = " + kbPaymentId + " for amount " + amount);
        } catch (ServiceException e) {
            throw new PaymentPluginApiException(ERROR_AUTHORIZATION_FAILED, "Failed to auth account " + kbAccountId + ", paymentId = " + kbPaymentId + " for amount " + amount + ": " + e.getMessage());
        } catch (SQLException e) {
            throw new PaymentPluginApiException(ERROR_SQL_READ, "Failed to auth account " + kbAccountId + ", paymentId = " + kbPaymentId + " for amount " + amount + ": " + e.getMessage());
        }
    }


    // Prototype, might need to introduce a first class citizen country code in account.
    private static String toCountryCode(final String country) {
        if (country.equalsIgnoreCase("germany")) {
            return "DE";
        }
        throw new RuntimeException("Not implemented for countries different than germany");
    }

    private static Amount toAmount(final BigDecimal value, final Currency currency) {
        Amount amount = new Amount();
        amount.setCurrency(currency.toString());
        amount.setValue(value.longValue() * 100);
        return amount;
    }


    private static PaymentPluginStatus getPaymentPluginStatus(final PaymentResult pr) {
        if (pr.getResultCode() != null && pr.getResultCode().equalsIgnoreCase("Authorised")) {
            return PaymentPluginStatus.PROCESSED;
        } else if (pr.getResultCode() != null && pr.getResultCode().equalsIgnoreCase("RedirectShopper")) {
            return PaymentPluginStatus.PENDING;
        } else {
            return PaymentPluginStatus.ERROR;
        }

    }

    private static PaymentTransactionInfoPlugin toPaymentInfoPlugin(final PaymentResult pr, final UUID KbPaymentId, final UUID KbPaymentTransactionId,
                                                                    final BigDecimal amount, final DateTime utcNow) {


        final PaymentPluginStatus pluginStatus = getPaymentPluginStatus(pr);
        final PluginProperty issuerUrl = new PluginProperty(PROP_ISSUER_URL, pr.getIssuerUrl(), false);
        final PluginProperty md = new PluginProperty(PROP_MD, pr.getMd(), false);
        final PluginProperty paReq = new PluginProperty(PROP_PA_REQ, pr.getPaRequest(), false);



    return new PaymentTransactionInfoPlugin() {
        @Override
        public UUID getKbPaymentId () {
            return KbPaymentId;
        }

        @Override
        public UUID getKbTransactionPaymentId () {
            return KbPaymentTransactionId;
        }

        @Override
        public TransactionType getTransactionType () {
            return null;
        }

        @Override
        public BigDecimal getAmount () {
            return amount;
        }

        @Override
        public Currency getCurrency () {
            return Currency.valueOf(pr.getDccAmount().getCurrency());
        }

        @Override
        public DateTime getCreatedDate () {
            return utcNow;
        }

        @Override
        public DateTime getEffectiveDate () {
            return utcNow;
        }

        @Override
        public PaymentPluginStatus getStatus () {
            return pluginStatus;
        }

        @Override
        public String getGatewayError () {
            return pr.getResultCode();
        }

        @Override
        public String getGatewayErrorCode () {
            return pr.getRefusalReason();
        }

        @Override
        public String getFirstPaymentReferenceId () {
            return pr.getPspReference();
        }

        @Override
        public String getSecondPaymentReferenceId () {
            return null;
        }

        @Override
        public List<PluginProperty> getProperties () {
            final ImmutableList.Builder<PluginProperty> propertiesBuilder = ImmutableList.builder();
            if (pluginStatus == PaymentPluginStatus.PENDING) {
                propertiesBuilder.add(issuerUrl)
                        .add(md)
                        .add(paReq);
            }
            return propertiesBuilder.build();
        }
    }

    ;
}

    private static Map<String, String> toMap(final Iterable<PluginProperty> properties, final Predicate predicate) {

        final ImmutableMap.Builder<String, String> result = ImmutableMap.builder();
        final Iterable<PluginProperty> filteredProperties = Iterables.filter(properties, predicate);
        for (PluginProperty prop : filteredProperties) {
            result.put(prop.getKey(), prop.getValue().toString());
        }
        return result.build();
    }

    private PaymentRequest createAuthorizeRequest(final UUID kbPaymentId, final BigDecimal amount, final Account account, Map<String, String> properties3DS) {
        final PaymentRequest request = new PaymentRequest();
        request.setMerchantAccount(merchantAccount);
        request.setReference(kbPaymentId.toString());
        request.setAmount(toAmount(amount, account.getCurrency()));

        final BrowserInfo browserInfo = new BrowserInfo();
        browserInfo.setAcceptHeader(properties3DS.get(PROP_ACCEPT_HEADER));
        browserInfo.setUserAgent(properties3DS.get(PROP_USER_AGENT));
        request.setBrowserInfo(browserInfo);

        // STEPH FIXME with real data.
        request.setCard(to3DSCard(account, null));
        return request;
    }

    //  HACK of course
    private static Card toCard(final Account account, final PaymentMethod paymentMethod) {
        Card card = new Card();
        card.setHolderName("Jean Pierre");
        card.setNumber("4111 1111 1111 1111");
        card.setCvc("737");
        card.setExpiryMonth("06");
        card.setExpiryYear("2016");
        return card;
    }

    //  HACK of course
    private static Card to3DSCard(final Account account, final PaymentMethod paymentMethod) {
        Card card = new Card();
        card.setHolderName("Jean Pierre");
        card.setNumber("4212 3456 7890 1237");
        card.setCvc("737");
        card.setExpiryMonth("06");
        card.setExpiryYear("2016");
        return card;
    }


    private PaymentRequest3D createAuthorize3DRequest(final UUID kbPaymentId, final Map<String, String> properties3DS) {

        final PaymentRequest3D request = new PaymentRequest3D();
        request.setMerchantAccount(merchantAccount);
        request.setReference(kbPaymentId.toString());

        final BrowserInfo browserInfo = new BrowserInfo();
        browserInfo.setAcceptHeader(properties3DS.get(PROP_ACCEPT_HEADER));
        browserInfo.setUserAgent(properties3DS.get(PROP_USER_AGENT));
        request.setBrowserInfo(browserInfo);

        request.setMd(properties3DS.get(PROP_MD));
        request.setPaResponse(properties3DS.get(PROP_PA_RES));
        return request;
    }


    @Override
    public PaymentTransactionInfoPlugin capturePayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbTransactionId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(UUID kbAccountId, UUID kbPaymentId, Iterable<PluginProperty> properties, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public Pagination<PaymentTransactionInfoPlugin> searchPayments(String searchKey, Long offset, Long limit, Iterable<PluginProperty> properties, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void addPaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, PaymentMethodPlugin paymentMethodProps, boolean setDefault, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public void deletePaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(UUID kbAccountId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void setDefaultPaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(UUID kbAccountId, boolean refreshFromGateway, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(String searchKey, Long offset, Long limit, Iterable<PluginProperty> properties, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void resetPaymentMethods(UUID kbAccountId, List<PaymentMethodInfoPlugin> paymentMethods, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(UUID kbAccountId, Iterable<PluginProperty> customFields, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public GatewayNotification processNotification(String notification, Iterable<PluginProperty> properties, CallContext context) throws PaymentPluginApiException {
        return null;
    }
}
