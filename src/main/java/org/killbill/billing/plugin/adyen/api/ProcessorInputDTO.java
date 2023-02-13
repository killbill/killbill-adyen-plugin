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

package org.killbill.billing.plugin.adyen.api;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.LocalDate;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.util.callcontext.TenantContext;

@Getter
@Setter
public class ProcessorInputDTO {

  private BigDecimal amount;

  private Currency currency;

  private String createdDate;

  private String kbAccountId;

  private String kbPaymentMethodId;

  private String kbTransactionId;

  private String pspReference;

  private String recurringData;

  private LocalDate salesDate;

  private PaymentMethod paymentMethod;

  private TransactionType transactionType;

  private TenantContext tenantContext;

  private Map<String, String> paymentMethodData;

  private Map<String, String> pluginConfiguration;

  private Map<String, String> paymentData;

  private Map<String, String> pluginProperties;
}
