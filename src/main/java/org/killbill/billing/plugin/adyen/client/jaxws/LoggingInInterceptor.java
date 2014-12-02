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

public class LoggingInInterceptor extends org.apache.cxf.interceptor.LoggingInInterceptor {

    private final Iterable<Obfuscator> obfuscators;

    public LoggingInInterceptor() {
        this(ImmutableList.<Obfuscator>of(new PayUObfuscator()));
    }

    public LoggingInInterceptor(final Iterable<Obfuscator> obfuscators) {
        this.obfuscators = obfuscators;
    }

    @Override
    protected String transform(final String originalLogString) {
        String result = originalLogString;

        for (final Obfuscator obfuscator : obfuscators) {
            result = obfuscator.obfuscateAdyenResponseLog(result);
        }

        return result;
    }
}
