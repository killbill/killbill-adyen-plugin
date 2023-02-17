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

import java.util.Map;
import java.util.UUID;
import org.killbill.billing.plugin.adyen.api.ProcessorInputDTO;
import org.killbill.billing.plugin.adyen.api.ProcessorOutputDTO;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;

public interface GatewayProcessor {

  public ProcessorOutputDTO processOneTimePayment(ProcessorInputDTO input);

  public ProcessorOutputDTO processPayment(ProcessorInputDTO input);

  public ProcessorOutputDTO refundPayment(ProcessorInputDTO input);

  public ProcessorOutputDTO voidPayment(ProcessorInputDTO input);

  public ProcessorInputDTO validateData(
      AdyenConfigurationHandler adyenConfigurationHandler,
      Map<String, String> properties,
      UUID context,
      UUID kbAccountId);
}
