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

package org.killbill.billing.plugin.adyen;

import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

// Assume all "slow" tests are integration (remote) tests. This is suboptimal and not true
// (see TestAdyenDao), but it's a quick workaround for unsupported multiple inheritance
public abstract class TestWithEmbeddedDBBase extends TestRemoteBase {

    protected AdyenDao dao;

    @BeforeClass(groups = "slow")
    public void setUpBeforeClass() throws Exception {
        super.setUpBeforeClass();

        dao = EmbeddedDbHelper.instance().startDb();
    }

    @BeforeMethod(groups = "slow")
    public void setUpBeforeMethod() throws Exception {
        EmbeddedDbHelper.instance().resetDB();
    }

    @AfterClass(groups = "slow")
    public void tearDownAfterClass() throws Exception {
        EmbeddedDbHelper.instance().stopDB();
    }
}
