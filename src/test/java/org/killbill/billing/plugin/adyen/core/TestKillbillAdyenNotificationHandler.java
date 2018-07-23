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

package org.killbill.billing.plugin.adyen.core;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.adyen.common.Amount;
import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.api.TestAdyenPaymentPluginApiBase;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.notification.AdyenNotificationHandler;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenNotificationsRecord;
import org.killbill.billing.util.callcontext.CallContext;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

public class TestKillbillAdyenNotificationHandler extends TestAdyenPaymentPluginApiBase {

    private Payment payment;
    private KillbillAdyenNotificationHandler killbillAdyenNotificationHandler;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        super.setUp();

        Mockito.when(killbillApi.getPaymentApi().createCapture(Mockito.<Account>any(),
                                                               Mockito.<UUID>any(),
                                                               Mockito.<BigDecimal>any(),
                                                               Mockito.<Currency>any(),
                                                               Mockito.<DateTime>any(),
                                                               Mockito.<String>any(),
                                                               Mockito.<Iterable<PluginProperty>>any(),
                                                               Mockito.<CallContext>any()))
               .then(new Answer<Payment>() {
                   @Override
                   public Payment answer(final InvocationOnMock invocation) throws Throwable {
                       final BigDecimal amount = (BigDecimal) invocation.getArguments()[2];
                       final Currency currency = (Currency) invocation.getArguments()[3];
                       final String paymentTransactionExternalKey = MoreObjects.firstNonNull((String) invocation.getArguments()[5], UUID.randomUUID().toString());

                       TestUtils.buildPaymentTransaction(payment, paymentTransactionExternalKey, TransactionType.CAPTURE, TransactionStatus.SUCCESS, amount, currency);

                       return payment;
                   }
               });

        TestUtils.buildPaymentMethod(account.getId(), account.getPaymentMethodId(), AdyenActivator.PLUGIN_NAME, killbillApi);
        payment = TestUtils.buildPayment(account.getId(), account.getPaymentMethodId(), account.getCurrency(), killbillApi);

        adyenConfigPropertiesConfigurationHandler = new AdyenConfigPropertiesConfigurationHandler(AdyenActivator.PLUGIN_NAME, killbillApi, logService, null);
        adyenConfigPropertiesConfigurationHandler.setDefaultConfigurable(new AdyenConfigProperties(new Properties()));

        killbillAdyenNotificationHandler = new KillbillAdyenNotificationHandler(adyenConfigPropertiesConfigurationHandler, killbillApi, dao, clock);
    }

    @Test(groups = "slow")
    public void testHandleAuthorizationCaptureSuccess() throws Exception {
        final boolean success = true;

        final NotificationRequestItem authItem = getNotificationRequestItem("AUTHORISATION", success);
        setupTransaction(TransactionType.AUTHORIZE, authItem);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PENDING);

        final PaymentTransactionInfoPlugin adyenPaymentTransactionInfoPlugin1 = new AdyenPaymentTransactionInfoPlugin(dao.getResponse(authItem.getPspReference()));
        Assert.assertNull(adyenPaymentTransactionInfoPlugin1.getGatewayError());
        Assert.assertNull(adyenPaymentTransactionInfoPlugin1.getGatewayErrorCode());

        killbillAdyenNotificationHandler.handleNotification(authItem);
        verifyLastNotificationRecorded(1);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        final PaymentTransactionInfoPlugin adyenPaymentTransactionInfoPlugin2 = new AdyenPaymentTransactionInfoPlugin(dao.getResponse(authItem.getPspReference()));
        Assert.assertNull(adyenPaymentTransactionInfoPlugin2.getGatewayError());
        Assert.assertNull(adyenPaymentTransactionInfoPlugin2.getGatewayErrorCode());

        // Capture done outside of Kill Bill
        final NotificationRequestItem captureItem = getNotificationRequestItem(authItem, "CAPTURE", success);

        killbillAdyenNotificationHandler.handleNotification(captureItem);
        verifyLastNotificationRecorded(2);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
    }

    @Test(groups = "slow")
    public void testHandleCaptureFailure() throws Exception {
        final boolean success = false;

        final NotificationRequestItem item = getNotificationRequestItem("CAPTURE", success);
        setupTransaction(TransactionType.CAPTURE, item);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PENDING);

        final PaymentTransactionInfoPlugin adyenPaymentTransactionInfoPlugin1 = new AdyenPaymentTransactionInfoPlugin(dao.getResponse(item.getPspReference()));
        Assert.assertNull(adyenPaymentTransactionInfoPlugin1.getGatewayError());
        Assert.assertNull(adyenPaymentTransactionInfoPlugin1.getGatewayErrorCode());

        killbillAdyenNotificationHandler.handleNotification(item);
        verifyLastNotificationRecorded(1);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PAYMENT_FAILURE);

        final PaymentTransactionInfoPlugin adyenPaymentTransactionInfoPlugin2 = new AdyenPaymentTransactionInfoPlugin(dao.getResponse(item.getPspReference()));
        Assert.assertNull(adyenPaymentTransactionInfoPlugin2.getGatewayError());
        Assert.assertNull(adyenPaymentTransactionInfoPlugin2.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testHandleRefundFailureAfterRefundSuccess() throws Exception {
        final boolean success = true;

        final NotificationRequestItem authItem = getNotificationRequestItem("AUTHORISATION", success);
        setupTransaction(TransactionType.AUTHORIZE, authItem);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(authItem);
        verifyLastNotificationRecorded(1);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        final NotificationRequestItem captureItem = getNotificationRequestItem(authItem, "CAPTURE", success);
        setupTransaction(TransactionType.CAPTURE, captureItem);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(captureItem);
        verifyLastNotificationRecorded(2);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);

        final NotificationRequestItem refundItem = getNotificationRequestItem(authItem, "REFUND", success);
        setupTransaction(TransactionType.REFUND, refundItem);

        Assert.assertEquals(payment.getTransactions().size(), 3);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(refundItem);
        verifyLastNotificationRecorded(3);

        Assert.assertEquals(payment.getTransactions().size(), 3);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionStatus(), TransactionStatus.SUCCESS);

        final PaymentTransactionInfoPlugin adyenPaymentTransactionInfoPlugin1 = new AdyenPaymentTransactionInfoPlugin(dao.getResponse(refundItem.getPspReference()));
        Assert.assertNull(adyenPaymentTransactionInfoPlugin1.getGatewayError());
        Assert.assertNull(adyenPaymentTransactionInfoPlugin1.getGatewayErrorCode());

        // The PSP reference of the refund is re-used
        final NotificationRequestItem refundFailedItem = getNotificationRequestItem(authItem, refundItem.getPspReference(), "REFUND_FAILED", success);

        killbillAdyenNotificationHandler.handleNotification(refundFailedItem);
        verifyLastNotificationRecorded(4);

        Assert.assertEquals(payment.getTransactions().size(), 3);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionStatus(), TransactionStatus.PAYMENT_FAILURE);

        final PaymentTransactionInfoPlugin adyenPaymentTransactionInfoPlugin2 = new AdyenPaymentTransactionInfoPlugin(dao.getResponse(refundItem.getPspReference()));
        Assert.assertNull(adyenPaymentTransactionInfoPlugin2.getGatewayError());
        Assert.assertNull(adyenPaymentTransactionInfoPlugin2.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testHandleRefundFailureAfterRefundSuccessV2() throws Exception {
        final boolean success = true;

        final NotificationRequestItem authItem = getNotificationRequestItem("AUTHORISATION", success);
        setupTransaction(TransactionType.AUTHORIZE, authItem);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(authItem);
        verifyLastNotificationRecorded(1);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        final NotificationRequestItem captureItem = getNotificationRequestItem(authItem, "CAPTURE", success);
        setupTransaction(TransactionType.CAPTURE, captureItem);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(captureItem);
        verifyLastNotificationRecorded(2);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);

        final NotificationRequestItem refundItem = getNotificationRequestItem(authItem, "REFUND", success);
        setupTransaction(TransactionType.REFUND, refundItem);

        Assert.assertEquals(payment.getTransactions().size(), 3);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(refundItem);
        verifyLastNotificationRecorded(3);

        Assert.assertEquals(payment.getTransactions().size(), 3);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionStatus(), TransactionStatus.SUCCESS);

        final PaymentTransactionInfoPlugin adyenPaymentTransactionInfoPlugin1 = new AdyenPaymentTransactionInfoPlugin(dao.getResponse(refundItem.getPspReference()));
        Assert.assertNull(adyenPaymentTransactionInfoPlugin1.getGatewayError());
        Assert.assertNull(adyenPaymentTransactionInfoPlugin1.getGatewayErrorCode());

        // The PSP reference of the refund is re-used and success is set to false
        final NotificationRequestItem refundFailedItem = getNotificationRequestItem(authItem, refundItem.getPspReference(), "REFUNDED_REVERSED", false);

        killbillAdyenNotificationHandler.handleNotification(refundFailedItem);
        verifyLastNotificationRecorded(4);

        Assert.assertEquals(payment.getTransactions().size(), 3);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionType(), TransactionType.REFUND);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionStatus(), TransactionStatus.PAYMENT_FAILURE);

        final PaymentTransactionInfoPlugin adyenPaymentTransactionInfoPlugin2 = new AdyenPaymentTransactionInfoPlugin(dao.getResponse(refundItem.getPspReference()));
        Assert.assertNull(adyenPaymentTransactionInfoPlugin2.getGatewayError());
        Assert.assertNull(adyenPaymentTransactionInfoPlugin2.getGatewayErrorCode());
    }

    @Test(groups = "slow")
    public void testHandleChargeback() throws Exception {
        final boolean success = true;

        final NotificationRequestItem purchaseItem = getNotificationRequestItem("AUTHORISATION", success);
        setupTransaction(TransactionType.PURCHASE, purchaseItem);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(purchaseItem);
        verifyLastNotificationRecorded(1);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        // Chargeback notification
        final NotificationRequestItem notificationOfChargebackItem = getNotificationRequestItem(purchaseItem, "NOTIFICATION_OF_CHARGEBACK", success);

        killbillAdyenNotificationHandler.handleNotification(notificationOfChargebackItem);
        // We'll find the payment, but there is no associated transaction (yet)
        verifyLastNotificationRecorded(2, null);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        // Request for information notification
        final NotificationRequestItem requestForInformationItem = getNotificationRequestItem(purchaseItem, "REQUEST_FOR_INFORMATION", success);

        killbillAdyenNotificationHandler.handleNotification(requestForInformationItem);
        // We'll find the payment, but there is no associated transaction (yet)
        verifyLastNotificationRecorded(3, null);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        // Chargeback done outside of Kill Bill
        final NotificationRequestItem chargebackItem = getNotificationRequestItem(purchaseItem, "CHARGEBACK", success);

        killbillAdyenNotificationHandler.handleNotification(chargebackItem);
        verifyLastNotificationRecorded(4);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CHARGEBACK);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
    }

    @Test(groups = "slow")
    public void testHandleChargebackOnCaptureWithSamePspReference() throws Exception {
        final boolean success = true;

        final NotificationRequestItem authItem = getNotificationRequestItem("AUTHORISATION", success);
        setupTransaction(TransactionType.AUTHORIZE, authItem);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(authItem);
        verifyLastNotificationRecorded(1);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        final NotificationRequestItem captureItem = getNotificationRequestItem(authItem, "CAPTURE", success);
        setupTransaction(TransactionType.CAPTURE, captureItem);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(captureItem);
        verifyLastNotificationRecorded(2);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);

        // Chargeback done outside of Kill Bill: note that the PSP reference is re-used by Adyen, make sure we don't link it to the capture transaction
        final NotificationRequestItem chargebackItem = getNotificationRequestItem(authItem, captureItem.getPspReference(), "CHARGEBACK", success);

        killbillAdyenNotificationHandler.handleNotification(chargebackItem);
        verifyLastNotificationRecorded(3);

        Assert.assertEquals(payment.getTransactions().size(), 3);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionType(), TransactionType.CHARGEBACK);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionStatus(), TransactionStatus.SUCCESS);
    }

    @Test(groups = "slow")
    public void testHandleChargebackOnCaptureDoneOutsideOfKillBillWithSamePspReference() throws Exception {
        final boolean success = true;

        final NotificationRequestItem authItem = getNotificationRequestItem("AUTHORISATION", success);
        setupTransaction(TransactionType.AUTHORIZE, authItem);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(authItem);
        verifyLastNotificationRecorded(1);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        // Capture done outside of Kill Bill
        final NotificationRequestItem captureItem = getNotificationRequestItem(authItem, "CAPTURE", success);

        killbillAdyenNotificationHandler.handleNotification(captureItem);
        verifyLastNotificationRecorded(2);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);

        // Chargeback done outside of Kill Bill: even though the same PSP reference as the capture is re-used,
        // the original response will be looked-up using the original PSP reference (capture not triggered by Kill Bill)
        final NotificationRequestItem chargebackItem = getNotificationRequestItem(authItem, captureItem.getPspReference(), "CHARGEBACK", success);

        killbillAdyenNotificationHandler.handleNotification(chargebackItem);
        verifyLastNotificationRecorded(3);

        Assert.assertEquals(payment.getTransactions().size(), 3);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.AUTHORIZE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CAPTURE);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionType(), TransactionType.CHARGEBACK);
        Assert.assertEquals(payment.getTransactions().get(2).getTransactionStatus(), TransactionStatus.SUCCESS);
    }

    @Test(groups = "slow")
    public void testHandleSEPAChargebackForPURCHASE() throws Exception {
        final Properties propertiesForSEPA = new Properties(properties);
        propertiesForSEPA.put("org.killbill.billing.plugin.adyen.chargebackAsFailurePaymentMethods", "ach,sepadirectdebit");
        final AdyenConfigPropertiesConfigurationHandler configurationHandlerForSEPA = new AdyenConfigPropertiesConfigurationHandler(AdyenActivator.PLUGIN_NAME,
                                                                                                                                    killbillApi,
                                                                                                                                    logService,
                                                                                                                                    null);
        configurationHandlerForSEPA.setDefaultConfigurable(new AdyenConfigProperties(propertiesForSEPA));
        final AdyenNotificationHandler handlerForSEPA = new KillbillAdyenNotificationHandler(configurationHandlerForSEPA,
                                                                                             killbillApi,
                                                                                             dao,
                                                                                             clock);

        final boolean success = true;

        final NotificationRequestItem authItem = getNotificationRequestItem("AUTHORISATION", success);
        setupTransaction(TransactionType.PURCHASE, authItem);

        handlerForSEPA.handleNotification(authItem);
        verifyLastNotificationRecorded(1);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        final NotificationRequestItem chargebackItem = getNotificationRequestItem(authItem, "CHARGEBACK", success);
        chargebackItem.setPaymentMethod("sepadirectdebit");

        handlerForSEPA.handleNotification(chargebackItem);
        verifyLastNotificationRecorded(2);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PAYMENT_FAILURE);
    }

    @Test(groups = "slow")
    public void testHandleChargebackReversed() throws Exception {
        final boolean success = true;

        final NotificationRequestItem authItem = getNotificationRequestItem("AUTHORISATION", success);
        setupTransaction(TransactionType.PURCHASE, authItem);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.PENDING);

        killbillAdyenNotificationHandler.handleNotification(authItem);
        verifyLastNotificationRecorded(1);

        Assert.assertEquals(payment.getTransactions().size(), 1);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);

        // Chargeback done outside of Kill Bill
        final NotificationRequestItem chargebackItem = getNotificationRequestItem(authItem, UUID.randomUUID().toString(), "CHARGEBACK", success);

        killbillAdyenNotificationHandler.handleNotification(chargebackItem);
        verifyLastNotificationRecorded(2);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CHARGEBACK);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.SUCCESS);

        // Chargeback reversal done outside of Kill Bill: no original PSP reference, but the PSP reference of the auth is re-used
        final NotificationRequestItem chargebackReversedItem = getNotificationRequestItem(null, authItem.getPspReference(), "CHARGEBACK_REVERSED", success);

        killbillAdyenNotificationHandler.handleNotification(chargebackReversedItem);
        verifyLastNotificationRecorded(3);

        Assert.assertEquals(payment.getTransactions().size(), 2);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionType(), TransactionType.PURCHASE);
        Assert.assertEquals(payment.getTransactions().get(0).getTransactionStatus(), TransactionStatus.SUCCESS);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionType(), TransactionType.CHARGEBACK);
        Assert.assertEquals(payment.getTransactions().get(1).getTransactionStatus(), TransactionStatus.PAYMENT_FAILURE);
    }

    @Test(groups = "slow")
    public void testReportAvailable() throws Exception {
        final boolean success = true;

        final NotificationRequestItem item = getNotificationRequestItem("REPORT_AVAILABLE", success);

        final List<PaymentTransaction> oldPaymentTransactions = ImmutableList.<PaymentTransaction>copyOf(payment.getTransactions());

        killbillAdyenNotificationHandler.handleNotification(item);

        final List<AdyenNotificationsRecord> notifications = dao.getNotifications();
        Assert.assertEquals(notifications.size(), 1);
        Assert.assertEquals(payment.getTransactions(), oldPaymentTransactions);
    }

    private void setupTransaction(final TransactionType transactionType, final NotificationRequestItem item) throws AccountApiException, PaymentApiException, SQLException {
        final PaymentTransaction paymentTransaction = TestUtils.buildPaymentTransaction(payment, transactionType, TransactionStatus.PENDING, BigDecimal.TEN, account.getCurrency());

        dao.addResponse(payment.getAccountId(),
                        paymentTransaction.getPaymentId(),
                        paymentTransaction.getId(),
                        paymentTransaction.getTransactionType(),
                        paymentTransaction.getAmount(),
                        paymentTransaction.getCurrency(),
                        new PurchaseResult(transactionType == TransactionType.AUTHORIZE ? PaymentServiceProviderResult.AUTHORISED : PaymentServiceProviderResult.RECEIVED, null, item.getPspReference(), null, null, null, null),
                        clock.getUTCNow(),
                        context.getTenantId());
    }

    private NotificationRequestItem getNotificationRequestItem(final String eventCode, final boolean success) {
        return getNotificationRequestItem(null, eventCode, success);
    }

    private NotificationRequestItem getNotificationRequestItem(@Nullable final NotificationRequestItem originalItem, final String eventCode, final boolean success) {
        final String pspReference = UUID.randomUUID().toString();
        return getNotificationRequestItem(originalItem, pspReference, eventCode, success);
    }

    private NotificationRequestItem getNotificationRequestItem(@Nullable final NotificationRequestItem originalItem, final String pspReference, final String eventCode, final boolean success) {
        final NotificationRequestItem item = new NotificationRequestItem();
        item.setEventCode(eventCode);
        item.setPspReference(pspReference);
        if (originalItem != null) {
            item.setOriginalReference(originalItem.getPspReference());
        }
        // Adyen doesn't require the merchant reference to be unique (modification transactions will default to the one from the auth)
        item.setMerchantReference(payment.getExternalKey());
        item.setSuccess(success);

        final Amount amount = new Amount();
        amount.setCurrency("EUR");
        amount.setValue(10l);
        item.setAmount(amount);

        return item;
    }

    private void verifyLastNotificationRecorded(final int nb) throws SQLException {
        verifyLastNotificationRecorded(nb, payment.getTransactions().get(payment.getTransactions().size() - 1).getId().toString());
    }

    private void verifyLastNotificationRecorded(final int nb, final String expectedTransactionId) throws SQLException {
        final List<AdyenNotificationsRecord> notifications = dao.getNotifications();
        Assert.assertEquals(notifications.size(), nb);
        Assert.assertEquals(notifications.get(nb - 1).getKbAccountId(), payment.getAccountId().toString());
        Assert.assertEquals(notifications.get(nb - 1).getKbPaymentId(), payment.getId().toString());
        Assert.assertEquals(notifications.get(nb - 1).getKbPaymentTransactionId(), expectedTransactionId);
    }
}
