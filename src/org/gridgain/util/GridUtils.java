// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.util;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Various utility methods.
 *
 * @author @java.author
 * @version @java.version
 */
public abstract class GridUtils {
    /**
     * Input-output closure with 2 input parameters.
     */
    public interface Closure2<E1, E2, R> {
        /**
         * Closure method.
         *
         * @param in1 Input parameter 1.
         * @param in2 Input parameter 2.
         * @return Closure result.
         */
        public R apply(E1 in1, E2 in2);
    }

    /**
     * Enum represents state of camel case parser.
     */
    private enum ParserState {
        /** State when no input symbols parsed yet. */
        START,

        /** First symbol parsed was capital. */
        CAPITAL,

        /** Parser is inside word token. */
        WORD,

        /** Parser is inside number. */
        NUM,

        /** Parser is inside abbreviation in capital letters. */
        ABBREVIATION
    }

    /**
     * Splits camel case string into parts..
     *
     * @param camelCase Camel case string.
     * @return List containing camel case parts.
     */
    public static List<String> camelCaseParts(String camelCase) {
        List<String> res = new LinkedList<String>();

        StringBuilder sb = new StringBuilder();

        ParserState state = ParserState.START;

        char pending = 0;

        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);

            switch (state) {
                case START:
                    sb.append(c);

                    if (Character.isLowerCase(c))
                        state = ParserState.WORD;
                    else if (Character.isUpperCase(c))
                        state = ParserState.CAPITAL;
                    else if (Character.isDigit(c))
                        state = ParserState.NUM;
                    else {
                        res.add(sb.toString());

                        sb.setLength(0);

                        // Remain in start state.
                    }
                    break;

                case CAPITAL:
                    if (Character.isLowerCase(c)) {
                        sb.append(c);

                        state = ParserState.WORD;
                    }
                    else if (Character.isUpperCase(c)) {
                        pending = c;

                        state = ParserState.ABBREVIATION;
                    }
                    else if (Character.isDigit(c)) {
                        res.add(sb.toString());

                        sb.setLength(0);

                        sb.append(c);

                        state = ParserState.NUM;
                    }
                    else {
                        res.add(sb.toString());

                        sb.setLength(0);

                        res.add(String.valueOf(c));

                        state = ParserState.START;
                    }
                    break;

                case WORD:
                    if (Character.isLowerCase(c))
                        sb.append(c);
                    else {
                        res.add(sb.toString());

                        sb.setLength(0);

                        state = ParserState.START;

                        // Unread.
                        i--;
                    }
                    break;

                case ABBREVIATION:
                    if (Character.isUpperCase(c)) {
                        sb.append(pending);

                        pending = c;
                    }
                    else if (Character.isLowerCase(c)) {
                        res.add(sb.toString());

                        sb.setLength(0);

                        sb.append(pending).append(c);

                        state = ParserState.WORD;
                    }
                    else {
                        sb.append(pending);

                        res.add(sb.toString());

                        sb.setLength(0);

                        state = ParserState.START;

                        // Unread.
                        i--;
                    }
                    break;

                case NUM:
                    if (Character.isDigit(c))
                        sb.append(c);
                    else {
                        res.add(sb.toString());

                        sb.setLength(0);

                        state = ParserState.START;

                        // Unread.
                        i--;
                    }
                    break;
            }
        }

        if (state == ParserState.ABBREVIATION)
            sb.append(pending);

        if (sb.length() > 0)
            res.add(sb.toString());

        return res;
    }

    /**
     * Un-capitalizes first letter of a given string.
     *
     * @param s Input string.
     * @return Capitalized string.
     */
    public static String unCapitalizeFirst(String s) {
        if (s.length() < 2)
            return s.toLowerCase();

        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    /**
     * Capitalizes first letter of a given string.
     *
     * @param s Input string.
     * @return Capitalized string.
     */
    public static String capitalizeFirst(String s) {
        if (s.length() < 2)
            return s.toUpperCase();

        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Transforms camel case string.
     *
     * @param camelCase Camel case string.
     * @param c Transform closure: 1-st parameter - camel case string part;
     *          2-nd parameter - part index;
     *          return - transformed part.
     * @return Concatenation of all transformed string parts.
     */
    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    public static String transformCamelCase(String camelCase, Closure2<String, Integer, String> c) {
        String[] parts = camelCaseParts(camelCase).toArray(new String[]{});
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++)
            sb.append(c.apply(parts[i], i));

        return sb.toString();
    }

    /**
     * Finds the minimal element from the specified array
     * in terms of specified closure.
     *
     * @param comp A 'minimum' definition, i.e. a comparator for
     *             passed values.
     * @param elems Elements, from which to find a minimum.
     * @param <T> Element type.
     * @return A minimal element or null, if an array is empty.
     */
    public static @Nullable <T> T min(Comparator<T> comp, T ... elems) {
        T ret = null;

        for (T elem : elems) {
            if (ret != null) {
                if (elem != null && comp.compare(elem, ret) < 0) // elem < ret
                    ret = elem;
            }
            else
                ret = elem;
        }

        return ret;
    }
}
