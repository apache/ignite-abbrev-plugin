/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.apache.ignite.idea.inspection.abbrev;

import com.intellij.lang.properties.psi.*;
import com.intellij.lang.properties.psi.impl.*;
import com.intellij.openapi.module.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.*;
import com.intellij.util.containers.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.*;

/**
 *
 */
public class GridAbbreviationConfig {

    private static final Key<Pair<Config, Long>> KEY = Key.create("GridAbbreviationConfig.KEY");

    private final Project project;

    private final Config defaultConfig;

    private PropertiesFileImpl cfgFile;

    private long lastVfsModification;

    public GridAbbreviationConfig(Project project) {
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
