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

package org.killbill.billing.plugin.adyen.client.jaxws;

import java.util.regex.Pattern;

public class PayUObfuscator implements Obfuscator {

    private static final Pattern PAY_U_PATTERN = Pattern.compile(">PayU\\.(ccnum|ccvv|ccexpmon|ccexpyr)</key>(\\s*)<value([^>]*)>[0-9]+</value>");
    private static final String PAY_U_REPLACEMENT = ">PayU.$1</key>$2<value$3>***</value>";

    @Override
    public String obfuscateAdyenResponseLog(final String response) {
        return PAY_U_PATTERN.matcher(response).replaceAll(PAY_U_REPLACEMENT);
    }

    @Override
    public String obfuscateAdyenRequestLog(final String request) {
        return request.replaceAll("expiryYear>[0-9]+</", "expiryYear>***</")
                      .replaceAll("expiryMonth>[0-9]+</", "expiryMonth>***</")
                      .replaceAll(">PayU\\.cardnum</([^>]+>\\s*<[^>]+)>[0-9X]+</", ">PayU.cardnum</$1>***</");
    }
}
