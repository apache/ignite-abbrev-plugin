// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.inspection.abbrev;

import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Abbreviation rules container.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridAbbreviationRules {
    /** Project home JVM property name. */
    private static final String PROJECT_HOME_PROP_NAME = "GRIDGAIN_PROJECT_HOME";

    /** File refresh frequency. */
    private static final int FILE_REFRESH_FREQ = 5000;

    /** Map from common words to abbreviations. */
    private volatile Map<String, String> abbrevMap = new ConcurrentHashMap<String, String>();

    /** Map from abbreviations to common words. */
    private volatile Map<String, String> revAbbrevMap = new ConcurrentHashMap<String, String>();

    /** File with abbreviation rules. */
    private File abbreviationFile;

    /** Initialization latch. */
    private CountDownLatch initLatch = new CountDownLatch(1);

    /** Singleton instance. */
    private static volatile GridAbbreviationRules instance;

    /** Init flag */
    private static AtomicBoolean initFlag = new AtomicBoolean();

    /** Singleton init latch. */
    private static CountDownLatch singletonInitLatch = new CountDownLatch(1);

    /** Abbreviation file used. */
    private final File file;

    /**
     * Singleton factory.
     *
     * @return Instance of abbreviation rules.
     */
    public static GridAbbreviationRules getInstance() {
        try {
            if (initFlag.compareAndSet(false, true)) {
                String ggHome = System.getProperty(PROJECT_HOME_PROP_NAME);

                if (ggHome != null) {
                    File abbrevFile =
                        new File(new File(ggHome), "idea" + File.separatorChar + "abbreviation.properties");

                    if (!abbrevFile.exists() || !abbrevFile.isFile()) {
                        System.err.println(
                            "${" + PROJECT_HOME_PROP_NAME + "}/idea/abbreviation.properties was not found. Using " +
                            "default abbreviation rules from plugin resources.");

                        instance = new GridAbbreviationRules(null);
                    }
                    else
                        instance = new GridAbbreviationRules(abbrevFile);
                }
                else
                    instance = new GridAbbreviationRules(null);

                singletonInitLatch.countDown();
            }
            else
                singletonInitLatch.await();

            return instance;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException("Interrupted while waiting for instance initialization");
        }
    }

    /**
     * Creates abbreviation rules depending on whether rules file found or not.
     *
     * @param file Abbreviations file or {@code null} if internal rules should be used.
     */
    private GridAbbreviationRules(@Nullable File file) {
        this.file = file;

        if (file == null) {
            InputStream is = getClass().getResourceAsStream("/abbreviation.properties");

            try {
                load(is);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    is.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    initLatch.countDown();
                }
            }
        }
        else {
            abbreviationFile = file;

            Thread fileWatchThread = new Thread(new AbbreviationFileWatcher());

            fileWatchThread.setDaemon(true);

            fileWatchThread.start();
        }
    }

    /**
     * Loads abbreviation rules from the input stream
     *
     * @param is Input stream.
     * @throws IOException If IO error occurs.
     */
    private void load(InputStream is) throws IOException {
        Properties props = new Properties();

        props.load(is);

        Map<String, String> refreshed = new ConcurrentHashMap<String, String>();
        Map<String, String> revRefreshed = new ConcurrentHashMap<String, String>();

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String)entry.getKey();
            String val = (String)entry.getValue();

            refreshed.put(key, val);
            revRefreshed.put(val, key);
        }

        abbrevMap = refreshed;
        revAbbrevMap = revRefreshed;
    }

    /**
     * Performs lookup of name part in abbreviation table.
     *
     * @param namePart Name part to lookup.
     * @return Abbreviation for given name or {@code null} if there is no such abbreviation.
     */
    @Nullable public String getAbbreviation(String namePart) {
        try {
            if (initLatch.getCount() == 1)
                initLatch.await();

            return abbrevMap.get(namePart.toLowerCase());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return null;
        }
    }

    /**
     * Performs lookup of abbreviated part in reverse abbreviation
     * table.
     *
     * @param abbrev Abbreviated string.
     * @return Unwrapped string.
     */
    @Nullable public String getUnwrapping(String abbrev) {
        try {
            if (initLatch.getCount() == 1)
                initLatch.await();

            return revAbbrevMap.get(abbrev.toLowerCase());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return null;
        }
    }

    /**
     * @return Abbreviation file used.
     */
    public File getFile() {
        return file;
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
     * Thread that watches for abbreviation file changes.
     */
    private class AbbreviationFileWatcher implements Runnable {
        /** File load timestamp. */
        private long loadTs;

        @Override public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    loadAbbreviations();

                    if (initLatch.getCount() == 1)
                        initLatch.countDown();

                    Thread.sleep(FILE_REFRESH_FREQ);
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Checks for file modification timestamp and refreshes abbreviation rules if needed.
         */
        @SuppressWarnings("unchecked")
        private void loadAbbreviations() {
            if (abbreviationFile.lastModified() > loadTs) {
                loadTs = abbreviationFile.lastModified();

                InputStream input = null;

                try {
                    input = new FileInputStream(abbreviationFile);

                    load(input);
                }
                catch (IOException ignored) {
                    // Just leave the last state.
                }
                finally {
                    if (input != null) {
                        try {
                            input.close();
                        }
                        catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    }
}
