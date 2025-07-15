/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.idea.intention;

import java.util.List;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

/** Tests Apache Ignite intentions. */
public class IgniteIntentionTest extends LightJavaCodeInsightFixtureTestCase {
    /** {@inheritDoc} */
    @Override protected String getTestDataPath() {
        return "src/test/resources/intention";
    }

    /** Tests {@link IgniteGetterSetterGenerator}. */
    public void testGetterSetter() {
        doTest("GetterSetter1", "Generate Apache Ignite style getter and setter");
        doTest("GetterSetter2", "Generate Apache Ignite style getter and setter");
        doTest("GetterSetter3", "Generate Apache Ignite style getter and setter");
    }

    /** Tests {@link IgniteGetterSetterSubstitutor}. */
    public void testGetterSetterSubstitutor() {
        doTest("GetterSetterSubstitutor", "Ignitify getters and setters");
    }

    /**
     * Given the name of a test file, runs comparing references inspection quick fix and tests
     * the results against a reference outcome file.
     * File name pattern 'foo.java' and 'foo.after.java' are matching before and after files
     * in the resources directory.
     *
     * @param testName  Test file name base
     * @param intention Quick fix name.
     */
    protected void doTest(String testName, String intention) {
        myFixture.configureByFile(testName + ".java");

        List<HighlightInfo> highlightInfos = myFixture.doHighlighting();
        assertFalse(highlightInfos.isEmpty());

        IntentionAction action = myFixture.getAvailableIntentions().stream()
            .filter(action1 -> intention.equals(action1.getText()))
            .findAny()
            .orElseThrow(() -> new AssertionError("Could not found intention " + intention));

        myFixture.launchAction(action);

        myFixture.checkResultByFile(testName + ".after.java");
    }
}
