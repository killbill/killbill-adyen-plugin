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

package org.killbill.billing.plugin.adyen.client.model;

public class Acquirer {

    private final String name;
    private final String mid;

    public Acquirer(final String name) {
        this(name, null);
    }

    public Acquirer(final String name, final String mid) {
        this.name = name;
        this.mid = mid;
    }

    public String getName() {
        return name;
    }

    public String getMid() {
        return mid;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Acquirer{");
        sb.append("name='").append(name).append('\'');
        sb.append(", mid='").append(mid).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Acquirer acquirer = (Acquirer) o;

        if (mid != null ? !mid.equals(acquirer.mid) : acquirer.mid != null) {
            return false;
        }
        if (name != null ? !name.equals(acquirer.name) : acquirer.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (mid != null ? mid.hashCode() : 0);
        return result;
    }
}
