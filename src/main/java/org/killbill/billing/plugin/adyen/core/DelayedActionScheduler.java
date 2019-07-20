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

import java.io.IOException;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueue;
import org.killbill.notificationq.api.NotificationQueueService;
import org.killbill.notificationq.api.NotificationQueueService.NoSuchNotificationQueue;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueAlreadyExists;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelayedActionScheduler {
    public static final String SERVICE_NAME = "adyen-service";
    public static final String QUEUE_NAME = "adyen-delayed-action";
    private static final Logger log = LoggerFactory.getLogger(DelayedActionScheduler.class);

    private final NotificationQueueService notificationQueueService;
    private final NotificationQueue delayedActionQueue;
    private AdyenPaymentPluginApi adyenPaymentPluginApi;

    public DelayedActionScheduler(final NotificationQueueService notificationQueueService,
                                  final AdyenDao adyenDao,
                                  final OSGIKillbillAPI osgiKillbillAPI,
                                  final AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler) throws NotificationQueueAlreadyExists {
        this.notificationQueueService = notificationQueueService;
        this.delayedActionQueue = notificationQueueService.createNotificationQueue(
                SERVICE_NAME,
                QUEUE_NAME,
                new NotificationQueueHandler() {
                    @Override
                    public void handleReadyNotification(final NotificationEvent notificationKey, final DateTime eventDateTime, final UUID userToken, final Long accountRecordId, final Long tenantRecordId) {
                        if (!(notificationKey instanceof DelayedActionEvent)) {
                            log.error("Received an unexpected event of type {}", notificationKey.getClass());
                            return;
                        }
                        if (adyenPaymentPluginApi == null) {
                            log.error("Received an event before being properly setup");
                            return;
                        }
                        try {
                            ((DelayedActionEvent) notificationKey).performAction(
                                    adyenPaymentPluginApi,
                                    adyenDao,
                                    osgiKillbillAPI,
                                    adyenConfigPropertiesConfigurationHandler);
                        } catch (RuntimeException ex) {
                            throw ex;
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });
    }

    public void setApi(AdyenPaymentPluginApi adyenPaymentPluginApi) {
        this.adyenPaymentPluginApi = adyenPaymentPluginApi;
    }

    public void start() {
        delayedActionQueue.startQueue();
    }

    public void scheduleAction(Duration delay, DelayedActionEvent action) {
        final DateTime targetTime = DateTime.now().plus(delay);
        try {
            delayedActionQueue.recordFutureNotification(targetTime, action, UUID.randomUUID(), 0L, 0L);
        } catch (IOException e) {
            log.warn("Failed to schedule action {} at {}: {}", action, targetTime, e.getMessage());
        }
    }

    public void stop() throws NoSuchNotificationQueue {
        if (delayedActionQueue != null) {
            delayedActionQueue.stopQueue();
            notificationQueueService.deleteNotificationQueue(delayedActionQueue.getServiceName(), delayedActionQueue.getQueueName());
        }
    }
}
