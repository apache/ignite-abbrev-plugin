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

package org.apache.ignite.idea.inspection;

import java.util.List;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.apache.ignite.idea.inspection.comment.IgniteCommentInspection;

/** Tests Apache Ignite inspections. */
public class IgniteInspectionTest extends LightJavaCodeInsightFixtureTestCase {
    /** {@inheritDoc} */
    @Override protected void setUp() throws Exception {
        super.setUp();

        myFixture.enableInspections(
            new IgniteCommentInspection(),
            new IgniteEmptyLineInspection(),
            new IgnitePlublicInterfaceMethodsInspection()
        );
    }

    /** {@inheritDoc} */
    @Override protected String getTestDataPath() {
        return "src/test/resources/inspection";
    }

    /** Tests {@link IgniteCommentInspection}. */
    public void testCommentInspection() {
        checkQuickFix("Comment1", "Add default comment");
        checkQuickFix("Comment2", "Add default comment");
        checkQuickFix("Comment3", "Add default comment");
        checkQuickFix("Comment4", "Add /** {@inheritDoc} */");
        checkQuickFix("Comment5", "Add default comment");
        checkQuickFix("Comment6", "Add empty comment");
        checkQuickFix("Comment7", "Add default comment");
    }

    /** Tests {@link IgnitePlublicInterfaceMethodsInspection}. */
    public void testPublicInterfaceMethodInspection() {
        checkQuickFix("PublicInterfaceMethod", generateFixAllIntentionNameByInspection(new IgnitePlublicInterfaceMethodsInspection()));
    }

    /** Tests {@link IgniteEmptyLineInspection}. */
    public void testEmptyLineInspection() {
        checkQuickFix("EmptyLine1", generateFixAllIntentionNameByInspection(new IgniteEmptyLineInspection()));
        checkQuickFix("EmptyLine2", generateFixAllIntentionNameByInspection(new IgniteEmptyLineInspection()));
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
    private void checkQuickFix(String testName, String intention) {
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

    /** */
    private String generateFixAllIntentionNameByInspection(InspectionProfileEntry inspection) {
        return "Fix all '" + inspection.getDisplayName() + "' problems in file";
    }
}
