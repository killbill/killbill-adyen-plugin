/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
 *
 * Ning licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.plugin.adyen.core.resources;

import com.google.inject.Inject;
import java.util.UUID;
import javax.inject.Singleton;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.mvc.Body;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.api.PluginCallContext;
import org.killbill.billing.plugin.core.resources.PluginHealthcheck;
import org.killbill.billing.util.callcontext.CallContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Path("/notification")
public class AdyenNotificationServlet extends PluginHealthcheck {
  private static final Logger logger = LoggerFactory.getLogger(AdyenNotificationServlet.class);
  private final OSGIKillbillClock clock;
  private final AdyenPaymentPluginApi adyenPaymentPluginApi;

  @Inject
  public AdyenNotificationServlet(
      final OSGIKillbillClock clock, final AdyenPaymentPluginApi adyenPaymentPluginApi) {
    this.clock = clock;
    this.adyenPaymentPluginApi = adyenPaymentPluginApi;
  }

  @POST
  public Result notificate(@Body String body) throws PaymentPluginApiException {
    logger.info("start notificate");
    final CallContext context =
        new PluginCallContext(
            AdyenActivator.PLUGIN_NAME,
            clock.getClock().getUTCNow(),
            UUID.randomUUID(),
            UUID.randomUUID());
    logger.info("start result of notificate");

    return Results.ok(adyenPaymentPluginApi.processNotification(body, null, context).getEntity());
  }
}
