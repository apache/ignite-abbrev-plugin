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

package org.apache.ignite.idea;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

/** Tests {@link IgniteMethodInsertionPointSelector}. */
public class IgniteMethodInsertionPointSelectorTest extends LightJavaCodeInsightFixtureTestCase {
    /** */
    private final IgniteMethodInsertionPointSelector selector = new IgniteMethodInsertionPointSelector();

    /** */
    private PsiMethod mthd;

    /** {@inheritDoc} */
    @Override public void setUp() throws Exception {
        super.setUp();

        mthd = JavaPsiFacade.getElementFactory(getProject()).createMethodFromText("void newMethod() {}", null);
    }

    /** */
    public void testSelectInsertionPointWithExternalMethods() {
        PsiClass psiClass = myFixture.addClass("""
            public class TestClass {
                public void writeExternal() {}
                public void readExternal() {}
                public void toString() {}
                public void customMethod() {}
            }
            """);

        PsiElement insertionPoint = selector.select(psiClass, mthd);

        PsiMethod toStringMethod = psiClass.findMethodsByName("writeExternal", false)[0];

        assertEquals(toStringMethod.getPrevSibling(), insertionPoint);
    }

    /** */
    public void testSelectInsertionPointWithoutStandardMethods() {
        PsiClass psiClass = myFixture.addClass("""
            public class TestClass {
                public void customMethod1() {}
                public void customMethod2() {}
            }
            """);

        PsiElement insertionPoint = selector.select(psiClass, mthd);

        PsiElement rBrace = psiClass.getRBrace();

        assertEquals(rBrace.getPrevSibling(), insertionPoint);
    }

    /** */
    public void testSelectInsertionPointInEmptyClass() {
        PsiClass psiClass = myFixture.addClass("public class TestClass {}");

        PsiElement insertionPoint = selector.select(psiClass, mthd);

        PsiElement rBrace = psiClass.getRBrace();

        assertNotNull(rBrace);
        assertEquals(rBrace.getPrevSibling(), insertionPoint);
    }

    /** */
    public void testSelectInsertionPointWithOnlyEquals() {
        PsiClass psiClass = myFixture.addClass("""
            public class TestClass {
            """);

        PsiElement insertionPoint = selector.select(psiClass, mthd);

        assertNull(insertionPoint);
    }
}
