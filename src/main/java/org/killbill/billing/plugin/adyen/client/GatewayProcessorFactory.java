/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
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
package org.killbill.billing.plugin.adyen.client;

import org.killbill.billing.plugin.adyen.core.AdyenConfigProperties;

public class GatewayProcessorFactory {
  private GatewayProcessorFactory() {}

  public static GatewayProcessor get(AdyenConfigProperties adyenConfigProperties) {
    AdyenSDKClientImpl httpClient = new AdyenSDKClientImpl(adyenConfigProperties);

    return new AdyenProcessorImpl(httpClient);
  }
}
