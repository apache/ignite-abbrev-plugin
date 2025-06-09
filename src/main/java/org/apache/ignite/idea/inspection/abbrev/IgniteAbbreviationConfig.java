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

package org.apache.ignite.idea.inspection.abbrev;

import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class IgniteAbbreviationConfig {

    private static final Key<Pair<Config, Long>> KEY = Key.create("IgniteAbbreviationConfig.KEY");

    private final Project project;

    private final Config defaultConfig;

    private PropertiesFileImpl cfgFile;

    private long lastVfsModification;

    public IgniteAbbreviationConfig(Project project) {
        this.project = project;

        try {
            InputStream is = getClass().getResourceAsStream("/abbreviation.properties");
            try {
                defaultConfig = new Config(is);
            }
            finally {
                is.close();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private PropertiesFileImpl findCfgFile() {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
                VirtualFile ruleFile = VfsUtil.findRelativeFile(root, "idea", "abbreviation.properties");

                if (ruleFile != null) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(ruleFile);

                    if (psiFile instanceof PropertiesFileImpl)
                        return (PropertiesFileImpl)psiFile;
                }
            }
        }

        return null;
    }

    @NotNull
    public Config getConfig() {
        if (cfgFile == null || !cfgFile.isValid()) {
            long currentModificationCount = VirtualFileManager.getInstance().getModificationCount();

            if (lastVfsModification != currentModificationCount) {
                lastVfsModification = currentModificationCount;

                cfgFile = findCfgFile();

                if (cfgFile == null)
                    return defaultConfig;
            }
            else {
                return defaultConfig;
            }
        }

        Pair<Config, Long> pair = cfgFile.getUserData(KEY);

        if (pair == null || cfgFile.getModificationStamp() != pair.second) {
            pair = new Pair<Config, Long>(new Config(cfgFile), cfgFile.getModificationStamp());

            cfgFile.putUserData(KEY, pair);
        }

        return pair.first;
    }

    /**
     * Performs lookup of abbreviated part in reverse abbreviation
     * table.
     *
     * @param abbrev Abbreviated string.
     * @return Unwrapped string.
     */
    @Nullable public String getUnwrapping(String abbrev) {
        return getConfig().revAbbrevMap.get(abbrev.toLowerCase());
    }

    /**
     * Performs lookup of name part in abbreviation table.
     *
     * @param namePart Name part to lookup.
     * @return Abbreviation for given name or {@code null} if there is no such abbreviation.
     */
    @Nullable public String getAbbreviation(String namePart) {
        return getConfig().abbrevMap.get(namePart.toLowerCase());
    }

    /**
     * Constructs abbreviated name from parts of wrong name.
     *
     * @param oldNameParts Split of variable name.
     * @return Abbreviated variable name.
     */
    public String replaceWithAbbreviations(List<String> oldNameParts) {
        StringBuilder sb = new StringBuilder();

        for (String part : oldNameParts) {
            String abbrev = getAbbreviation(part);

            if (abbrev == null)
                sb.append(part);
            else {
                // Only the following cases are possible: count, Count and COUNT since
                // parser splits tokens based on this rule.
                int pos = sb.length();

                sb.append(abbrev);

                if (Character.isUpperCase(part.charAt(0))) {
                    sb.setCharAt(pos, Character.toUpperCase(sb.charAt(pos)));

                    pos++;

                    if (Character.isUpperCase(part.charAt(part.length() - 1))) {
                        // Full abbreviation, like COUNT
                        while (pos < sb.length()) {
                            sb.setCharAt(pos, Character.toUpperCase(sb.charAt(pos)));

                            pos++;
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     *
     */
    private static class Config {
        /** Map from common words to abbreviations. */
        private final Map<String, String> abbrevMap = new HashMap<String, String>();

        /** Map from abbreviations to common words. */
        private final Map<String, String> revAbbrevMap = new HashMap<String, String>();

        private Config(PropertiesFileImpl file) {
            for (Map.Entry<String, String> entry : file.getNamesMap().entrySet()) {
                abbrevMap.put(entry.getKey(), entry.getValue());
                revAbbrevMap.put(entry.getValue(), entry.getKey());
            }
        }

        private Config(InputStream is) throws IOException {
            Properties props = new Properties();

            props.load(is);

            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String key = (String)entry.getKey();
                String val = (String)entry.getValue();

                abbrevMap.put(key, val);
                revAbbrevMap.put(val, key);
            }
        }
    }
}
