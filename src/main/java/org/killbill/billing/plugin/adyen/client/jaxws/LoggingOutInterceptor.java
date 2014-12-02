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

import com.google.common.collect.ImmutableList;

public class LoggingOutInterceptor extends org.apache.cxf.interceptor.LoggingOutInterceptor {

    private final Iterable<Obfuscator> obfuscators;

    public LoggingOutInterceptor() {
        this(ImmutableList.<Obfuscator>of(new PayUObfuscator()));
    }

    public LoggingOutInterceptor(final Iterable<Obfuscator> obfuscators) {
        this.obfuscators = obfuscators;
    }

    @Override
    protected String transform(final String originalLogString) {
        String result = originalLogString.replaceAll("cvc>[0-9]+</", "cvc>***</")
                                         .replaceAll("number>[0-9]+</", "number>***</")
                                         .replaceAll("bankAccountNumber>[0-9]+</", "bankAccountNumber>***</");

        for (final Obfuscator obfuscator : obfuscators) {
            result = obfuscator.obfuscateAdyenRequestLog(result);
        }

        return result;
    }
}
