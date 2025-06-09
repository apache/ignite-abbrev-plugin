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

package org.apache.ignite.idea.action;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.ignite.idea.intention.IgniteGetterSetterGenerator;

/**
 * Action for generating Apache Ignite style getters and setters.
 */
public class IgniteGetterSetterAction extends AnAction {
    /** {@inheritDoc} */
    public void actionPerformed(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        final Editor editor = e.getData(LangDataKeys.EDITOR);
        final Project project = e.getProject();

        if (psiFile == null || editor == null || project == null)
            return;

        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        final PsiClass psiCls = PsiTreeUtil.getParentOfType(element, PsiClass.class, true);

        if (psiCls == null)
            return;

        final IgniteGetterSetterGenerator gen = new IgniteGetterSetterGenerator();

        Collection<PsiFieldMember> mFields = new LinkedList<PsiFieldMember>();

        for (PsiField field : psiCls.getFields()) {
            if (gen.isAvailable(project, editor, field))
                mFields.add(new PsiFieldMember(field));
        }

        MemberChooser<PsiFieldMember> mc = new MemberChooser<PsiFieldMember>(
            mFields.toArray(new PsiFieldMember[mFields.size()]), true, true, project);

        mc.setCopyJavadocVisible(false);

        if (!mc.showAndGet())
            return;

        final List<PsiFieldMember> members = mc.getSelectedElements();

        if (members == null)
            return;

        // Generate code.
        runWriteCommand(project, new Runnable() {
            @Override public void run() {
                for (PsiFieldMember fm : members) {
                    gen.invoke(project, editor, fm.getPsiElement());

                    CodeStyleManager codeStyleMan = CodeStyleManager.getInstance(project);

                    codeStyleMan.reformat(psiCls);
                }
            }
        });
    }

    /**
     * Runs a write action, wrapped in a command.
     *
     * @param project Project.
     * @param runnable Action to run.
     */
    private void runWriteCommand(Project project, final Runnable runnable) {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override public void run() {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }
        }, null, null);
    }
}
