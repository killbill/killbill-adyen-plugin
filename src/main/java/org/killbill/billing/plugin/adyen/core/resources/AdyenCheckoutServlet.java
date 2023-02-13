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
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jooby.mvc.Local;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.api.PluginCallContext;
import org.killbill.billing.plugin.core.PluginServlet;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.callcontext.CallContext;

@Singleton
@Path("/checkout")
public class AdyenCheckoutServlet extends PluginServlet {

  private final OSGIKillbillClock clock;
  private final AdyenCheckoutService service;

  @Inject
  public AdyenCheckoutServlet(final OSGIKillbillClock clock, AdyenCheckoutService service) {
    this.clock = clock;
    this.service = service;
  }

  @POST
  public Map<String, String> createSession(
      @Named("kbAccountId") final UUID kbAccountId,
      @Named("amount") final BigDecimal amount,
      @Named("kbPaymentMethodId") final UUID kbPaymentMethodId,
      @Local @Named("killbill_tenant") final Tenant tenant)
      throws PaymentPluginApiException {

    final CallContext context =
        new PluginCallContext(
            AdyenActivator.PLUGIN_NAME, clock.getClock().getUTCNow(), kbAccountId, tenant.getId());

    return service.createSession(kbAccountId, context, amount, kbPaymentMethodId, tenant.getId());
  }
}
