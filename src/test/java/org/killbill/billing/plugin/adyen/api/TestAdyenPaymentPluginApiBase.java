/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
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

import java.math.BigDecimal;
import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.TestWithEmbeddedDBBase;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;

import com.google.common.collect.ImmutableList;

public class TestAdyenPaymentPluginApiBase extends TestWithEmbeddedDBBase {

    protected CallContext context;
    protected Payment payment;
    protected PaymentTransaction paymentTransaction;
    protected PaymentMethod paymentMethod;
    protected AdyenPaymentPluginApi adyenPaymentPluginApi;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        final Clock clock = new DefaultClock();

        context = Mockito.mock(CallContext.class);
        Mockito.when(context.getTenantId()).thenReturn(UUID.randomUUID());

        final Account account = TestUtils.buildAccount(DEFAULT_CURRENCY, DEFAULT_COUNTRY);
        payment = TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency());
        paymentTransaction = buildPaymentTransaction(TransactionType.PURCHASE);
        paymentMethod = TestUtils.buildPaymentMethod(account.getId(), account.getPaymentMethodId(), AdyenActivator.PLUGIN_NAME);
        final OSGIKillbillAPI killbillApi = TestUtils.buildOSGIKillbillAPI(account, payment, paymentMethod);
        Mockito.when(killbillApi.getPaymentApi().getAccountPaymentMethods(Mockito.eq(account.getId()), Mockito.anyBoolean(), Mockito.<Iterable<PluginProperty>>any(), Mockito.<TenantContext>any())).thenReturn(ImmutableList.<PaymentMethod>of(paymentMethod));
        Mockito.when(killbillApi.getPaymentApi().createAuthorization(Mockito.<Account>any(), Mockito.<UUID>any(), Mockito.<UUID>any(), Mockito.<BigDecimal>any(), Mockito.<Currency>any(), Mockito.<String>any(), Mockito.<String>any(), Mockito.<Iterable<PluginProperty>>any(), Mockito.<CallContext>any())).then(new Answer<Payment>() {
            @Override
            public Payment answer(final InvocationOnMock invocation) throws Throwable {
                Mockito.when(paymentTransaction.getTransactionType()).thenReturn(TransactionType.AUTHORIZE);
                Mockito.when(paymentTransaction.getTransactionStatus()).thenReturn(TransactionStatus.PENDING);

                adyenPaymentPluginApi.authorizePayment(payment.getAccountId(),
                                                       payment.getId(),
                                                       paymentTransaction.getId(),
                                                       payment.getPaymentMethodId(),
                                                       paymentTransaction.getAmount(),
                                                       paymentTransaction.getCurrency(),
                                                       (Iterable<PluginProperty>) invocation.getArguments()[invocation.getArguments().length - 2],
                                                       context);
                return payment;
            }
        });
        Mockito.when(killbillApi.getPaymentApi().createPurchase(Mockito.<Account>any(), Mockito.<UUID>any(), Mockito.<UUID>any(), Mockito.<BigDecimal>any(), Mockito.<Currency>any(), Mockito.<String>any(), Mockito.<String>any(), Mockito.<Iterable<PluginProperty>>any(), Mockito.<CallContext>any())).then(new Answer<Payment>() {
            @Override
            public Payment answer(final InvocationOnMock invocation) throws Throwable {
                Mockito.when(paymentTransaction.getTransactionType()).thenReturn(TransactionType.PURCHASE);
                Mockito.when(paymentTransaction.getTransactionStatus()).thenReturn(TransactionStatus.PENDING);

                adyenPaymentPluginApi.purchasePayment(payment.getAccountId(),
                                                      payment.getId(),
                                                      paymentTransaction.getId(),
                                                      payment.getPaymentMethodId(),
                                                      paymentTransaction.getAmount(),
                                                      paymentTransaction.getCurrency(),
                                                      (Iterable<PluginProperty>) invocation.getArguments()[invocation.getArguments().length - 2],
                                                      context);
                return payment;
            }
        });

        final OSGIKillbillLogService logService = TestUtils.buildLogService();

        final OSGIConfigPropertiesService configPropertiesService = Mockito.mock(OSGIConfigPropertiesService.class);
        adyenPaymentPluginApi = new AdyenPaymentPluginApi(adyenConfigurationHandler, adyenHostedPaymentPageConfigurationHandler, adyenRecurringConfigurationHandler, killbillApi, configPropertiesService, logService, clock, dao);
    }

    protected PaymentTransaction buildPaymentTransaction(final TransactionType transactionType) {
        return buildPaymentTransaction(transactionType, BigDecimal.TEN);
    }

    protected PaymentTransaction buildPaymentTransaction(final TransactionType transactionType, final BigDecimal amount) {
        final PaymentTransaction paymentTransaction = TestUtils.buildPaymentTransaction(payment, transactionType, DEFAULT_CURRENCY);
        Mockito.when(paymentTransaction.getAmount()).thenReturn(amount);
        return paymentTransaction;
    }
}
