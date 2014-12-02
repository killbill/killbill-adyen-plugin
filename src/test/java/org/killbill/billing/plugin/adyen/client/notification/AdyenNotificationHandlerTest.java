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

import java.util.LinkedList;
import java.util.List;

import org.killbill.adyen.notification.NotificationRequestItem;

public class AdyenNotificationHandlerTest implements AdyenNotificationHandler {

    private final List<NotificationRequestItem> authorisationSuccessItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> authorisationFailureItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> captureSuccessItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> captureFailureItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> captureFailedItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> cancellationSuccessItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> cancellationFailureItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> chargebackItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> chargebackReversedItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> refundSuccessItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> refundFailureItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> refundedReversedItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> refundFailedItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> notificationOfChargebackItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> cancelOrRefundSuccessItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> notificationOfFraudItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> cancelOrRefundFailureItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> disputeItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> requestForInformationItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> reportAvailableItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> notificationtestItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> recurringReceivedItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> cancelReceivedItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> recurringDetailDisabledItems = new LinkedList<NotificationRequestItem>();
    private final List<NotificationRequestItem> recurringForUserDisabledItems = new LinkedList<NotificationRequestItem>();

    @Override
    public boolean canHandleNotification(final NotificationRequestItem item) {
        return true;
    }

    @Override
    public void authorisationSuccess(final NotificationRequestItem item) {
        authorisationSuccessItems.add(item);
    }

    @Override
    public void authorisationFailure(final NotificationRequestItem item) {
        authorisationFailureItems.add(item);
    }

    @Override
    public void captureSuccess(final NotificationRequestItem item) {
        captureSuccessItems.add(item);
    }

    @Override
    public void captureFailure(final NotificationRequestItem item) {
        captureFailureItems.add(item);
    }

    @Override
    public void captureFailed(final NotificationRequestItem item) {
        captureFailedItems.add(item);
    }

    @Override
    public void cancellationSuccess(final NotificationRequestItem item) {
        cancellationSuccessItems.add(item);
    }

    @Override
    public void cancellationFailure(final NotificationRequestItem item) {
        cancellationFailureItems.add(item);
    }

    @Override
    public void chargeback(final NotificationRequestItem item) {
        chargebackItems.add(item);
    }

    @Override
    public void chargebackReversed(final NotificationRequestItem item) {
        chargebackReversedItems.add(item);
    }

    @Override
    public void refundSuccess(final NotificationRequestItem item) {
        refundSuccessItems.add(item);
    }

    @Override
    public void refundFailure(final NotificationRequestItem item) {
        refundFailureItems.add(item);
    }

    @Override
    public void refundedReversed(final NotificationRequestItem item) {
        refundedReversedItems.add(item);
    }

    @Override
    public void refundFailed(final NotificationRequestItem item) {
        refundFailedItems.add(item);
    }

    @Override
    public void notificationOfChargeback(final NotificationRequestItem item) {
        notificationOfChargebackItems.add(item);
    }

    @Override
    public void cancelOrRefundSuccess(final NotificationRequestItem item) {
        cancelOrRefundSuccessItems.add(item);
    }

    @Override
    public void notificationOfFraud(final NotificationRequestItem item) {
        notificationOfFraudItems.add(item);
    }

    @Override
    public void requestForInformation(final NotificationRequestItem item) {
        requestForInformationItems.add(item);
    }

    @Override
    public void cancelOrRefundFailure(final NotificationRequestItem item) {
        cancelOrRefundFailureItems.add(item);
    }

    @Override
    public void dispute(final NotificationRequestItem item) {
        disputeItems.add(item);
    }

    @Override
    public void reportAvailable(final NotificationRequestItem item) {
        reportAvailableItems.add(item);
    }

    @Override
    public void notificationtest(final NotificationRequestItem item) {
        notificationtestItems.add(item);
    }

    @Override
    public void recurringReceived(final NotificationRequestItem item) {
        recurringReceivedItems.add(item);
    }

    @Override
    public void cancelReceived(final NotificationRequestItem item) {
        cancelReceivedItems.add(item);
    }

    @Override
    public void recurringDetailDisabled(final NotificationRequestItem item) {
        recurringDetailDisabledItems.add(item);
    }

    @Override
    public void recurringForUserDisabled(final NotificationRequestItem item) {
        recurringForUserDisabledItems.add(item);
    }

    public List<NotificationRequestItem> getAuthorisationSuccessItems() {
        return authorisationSuccessItems;
    }

    public List<NotificationRequestItem> getAuthorisationFailureItems() {
        return authorisationFailureItems;
    }

    public List<NotificationRequestItem> getCaptureSuccessItems() {
        return captureSuccessItems;
    }

    public List<NotificationRequestItem> getCaptureFailureItems() {
        return captureFailureItems;
    }

    public List<NotificationRequestItem> getCaptureFailedItems() {
        return captureFailedItems;
    }

    public List<NotificationRequestItem> getCancellationSuccessItems() {
        return cancellationSuccessItems;
    }

    public List<NotificationRequestItem> getCancellationFailureItems() {
        return cancellationFailureItems;
    }

    public List<NotificationRequestItem> getChargebackItems() {
        return chargebackItems;
    }

    public List<NotificationRequestItem> getChargebackReversedItems() {
        return chargebackReversedItems;
    }

    public List<NotificationRequestItem> getRefundSuccessItems() {
        return refundSuccessItems;
    }

    public List<NotificationRequestItem> getRefundFailureItems() {
        return refundFailureItems;
    }

    public List<NotificationRequestItem> getRefundedReversedItems() {
        return refundedReversedItems;
    }

    public List<NotificationRequestItem> getRefundFailedItems() {
        return refundFailedItems;
    }

    public List<NotificationRequestItem> getNotificationOfChargebackItems() {
        return notificationOfChargebackItems;
    }

    public List<NotificationRequestItem> getCancelOrRefundSuccessItems() {
        return cancelOrRefundSuccessItems;
    }

    public List<NotificationRequestItem> getNotificationOfFraudItems() {
        return notificationOfFraudItems;
    }

    public List<NotificationRequestItem> getCancelOrRefundFailureItems() {
        return cancelOrRefundFailureItems;
    }

    public List<NotificationRequestItem> getDisputeItems() {
        return disputeItems;
    }

    public List<NotificationRequestItem> getRequestForInformationItems() {
        return requestForInformationItems;
    }

    public List<NotificationRequestItem> getReportAvailableItems() {
        return reportAvailableItems;
    }

    public List<NotificationRequestItem> getNotificationtestItems() {
        return notificationtestItems;
    }

    public List<NotificationRequestItem> getRecurringReceivedItems() {
        return recurringReceivedItems;
    }

    public List<NotificationRequestItem> getCancelReceivedItems() {
        return cancelReceivedItems;
    }

    public List<NotificationRequestItem> getRecurringDetailDisabledItems() {
        return recurringDetailDisabledItems;
    }

    public List<NotificationRequestItem> getRecurringForUserDisabledItems() {
        return recurringForUserDisabledItems;
    }
}
