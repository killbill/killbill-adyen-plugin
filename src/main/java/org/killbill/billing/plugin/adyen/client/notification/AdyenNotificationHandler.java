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

package org.killbill.billing.plugin.adyen.client.notification;

import org.killbill.adyen.notification.NotificationRequestItem;

public interface AdyenNotificationHandler {

    boolean canHandleNotification(NotificationRequestItem item);

    void authorisationSuccess(NotificationRequestItem item);

    void authorisationFailure(NotificationRequestItem item);

    void captureSuccess(NotificationRequestItem item);

    void captureFailure(NotificationRequestItem item);

    void captureFailed(NotificationRequestItem item);

    void cancellationSuccess(NotificationRequestItem item);

    void cancellationFailure(NotificationRequestItem item);

    void chargeback(NotificationRequestItem item);

    void chargebackReversed(NotificationRequestItem item);

    void refundSuccess(NotificationRequestItem item);

    void refundFailure(NotificationRequestItem item);

    void refundedReversed(NotificationRequestItem item);

    void refundFailed(NotificationRequestItem item);

    void notificationOfChargeback(NotificationRequestItem item);

    void cancelOrRefundSuccess(NotificationRequestItem item);

    void notificationOfFraud(NotificationRequestItem item);

    void requestForInformation(NotificationRequestItem item);

    void cancelOrRefundFailure(NotificationRequestItem item);

    void dispute(NotificationRequestItem item);

    void reportAvailable(NotificationRequestItem item);

    void notificationtest(NotificationRequestItem item);

    void recurringReceived(NotificationRequestItem item);

    void cancelReceived(NotificationRequestItem item);

    void recurringDetailDisabled(NotificationRequestItem item);

    void recurringForUserDisabled(NotificationRequestItem item);
}
