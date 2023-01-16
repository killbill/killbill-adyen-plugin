/*
 * Copyright 2021 Wovenware, Inc
 *
 * Wovenware licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.api;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;

@Getter
@Setter
public class ProcessorOutputDTO {

  private Map<String, String> additionalData;
  private TransactionType type;
  private PaymentPluginStatus status;
  private String gatewayError;
  private String gatewayErrorCode;
  private String firstPaymentReferenceId;
  private String secondPaymentReferenceId;
  private String pspReferenceCode;
  private DateTime transactionDate;
}
