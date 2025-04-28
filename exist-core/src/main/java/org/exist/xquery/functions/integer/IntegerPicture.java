/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.xquery.functions.integer;

import com.ibm.icu.text.RuleBasedNumberFormat;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.Expression;
import org.exist.xquery.XPathException;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format numbers according to the rules for
 * <a href="https://www.w3.org/TR/xpath-functions-31/#formatting-integers">format-integer</a>
 */
public abstract class IntegerPicture {

    static final BigInteger TEN = BigInteger.valueOf(10L);

    //This contains \\v (vertical whitespace characters) so anything with vertical white space isn't a pattern
    //It also disallows 0 instances of the pattern
    //When decimal digit pattern doesn't match, we end up falling into a standard default.
    static final Pattern decimalDigitPattern = Pattern.compile("^((\\p{Nd}|#|[^\\p{N}\\p{L}\\v])+)$", Pattern.UNICODE_CHARACTER_CLASS);
    static final Pattern invalidDigitPattern = Pattern.compile("(\\p{Nd})");

    /**
     * The value of $picture consists of a primary format token,
     * optionally followed by a format modifier.
     *
     * @param pictureFormat the format to use - choose which sub picture is needed
     * @return the right picture to handle this format
     * @throws XPathException if the format is not a known/valid form of picture format
     */
    public static IntegerPicture fromString(final String pictureFormat) throws XPathException {

        final String primaryFormatToken;
        final FormatModifier formatModifier;

        final int splitPosition = pictureFormat.lastIndexOf(';');
        if (splitPosition < 0) {
            primaryFormatToken = pictureFormat;
            formatModifier = new FormatModifier("");
        } else {
            primaryFormatToken = pictureFormat.substring(0, splitPosition);
            formatModifier = new FormatModifier(pictureFormat.substring(splitPosition + 1));
        }
        if (primaryFormatToken.isEmpty()) {
            throw new XPathException((Expression) null, ErrorCodes.FODF1310, "Invalid (empty) primary format token in integer format token: " + primaryFormatToken);
        }

        // type 1 matcher (some digits)
        final Matcher decimalDigitMatcher = IntegerPicture.decimalDigitPattern.matcher(primaryFormatToken);
        if (decimalDigitMatcher.matches()) {
            return new DigitsIntegerPicture(primaryFormatToken, formatModifier);
        }

        // incorrect type 1 matcher (and not anything else)
        final Matcher invalidDigitMatcher = IntegerPicture.invalidDigitPattern.matcher(primaryFormatToken);
        if (invalidDigitMatcher.find()) {
            throw new XPathException((Expression) null, ErrorCodes.FODF1310, "Invalid primary format token is not a valid decimal digital pattern: " + primaryFormatToken);
        }

        // specifically defined format token rules 2-8
        // <a href="https://www.w3.org/TR/xpath-functions-31/#formatting-integers"/>
        switch (primaryFormatToken) {
            case "A":
                return new SequenceIntegerPicture('A');
            case "a":
                return new SequenceIntegerPicture('a');
            case "i":
                return new RomanIntegerPicture(false/*isUpper*/);
            case "I":
                return new RomanIntegerPicture(true/*isUpper*/);
            case "W":
                return new WordPicture(WordPicture.CaseAndCaps.UPPER, formatModifier);
            case "w":
                return new WordPicture(WordPicture.CaseAndCaps.LOWER, formatModifier);
            case "Ww":
                return new WordPicture(WordPicture.CaseAndCaps.CAPITALIZED, formatModifier);
            default:
                break;
        }

        // Rule 9 - sequences
        // <a href="https://www.w3.org/TR/xpath-functions-31/#formatting-integers"/>
        final List<Integer> codePoints = IntegerPicture.codePoints(primaryFormatToken);
        final Optional<IntegerPicture> numberingPicture = NumberingPicture.fromIndexCodePoint(codePoints.get(0), formatModifier);

        return numberingPicture.orElse(IntegerPicture.defaultPictureWithModifier(formatModifier));
    }

    static IntegerPicture defaultPictureWithModifier(final FormatModifier formatModifier) throws XPathException {
        return new DigitsIntegerPicture("1", formatModifier);
    }

    /**
     * Format an integer according to the picture and language with which this was constructed
     *
     * @param bigInteger the integer to format
     * @param locale of the language to use in formatting
     * @return a string containing the formatted integer
     * @throws XPathException if the locale is ill-formed
     */
    protected abstract String formatInteger(BigInteger bigInteger, Locale locale) throws XPathException;

    private static Locale getLocaleFromLanguages(final List<String> languages) throws XPathException {

        IllformedLocaleException languageILE = null;
        for (final String language : languages) {
            final Locale.Builder localeBuilder = new Locale.Builder();
            try {
                localeBuilder.setLanguage(language);
                return localeBuilder.build();
            } catch (final IllformedLocaleException ile) {
                languageILE = ile;
            }
        }
        assert languageILE != null;
        throw new XPathException((Expression) null, ErrorCodes.FODF1310, languageILE.getMessage());
    }

    public final String formatInteger(final BigInteger bigInteger, final List<String> languages) throws XPathException {

        final Locale locale = IntegerPicture.getLocaleFromLanguages(languages);
        return formatInteger(bigInteger, locale);
    }

    /**
     * Convert a string into a list of unicode code points
     *
     * @param s the input string
     * @return a list of the codepoints forming the string
     */
    protected static List<Integer> codePoints(final String s) {
        final List<Integer> codePointList = new ArrayList<>(s.length());
        int i = 0;
        while (i < s.length()) {
            final int codePoint = Character.codePointAt(s, i);
            i += Character.charCount(codePoint);
            codePointList.add(codePoint);
        }
        return codePointList;
    }

    protected static String fromCodePoint(final int codePoint) {
        final StringBuilder sb = new StringBuilder();
        for (final char c : Character.toChars(codePoint)) {
            sb.append(c);
        }
        return sb.toString();
    }

    protected static String ordinalSuffix(final int value, final Locale locale) {
        final RuleBasedNumberFormat ruleBasedNumberFormat = new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.ORDINAL);
        final StringBuilder sb = new StringBuilder(ruleBasedNumberFormat.format(value)).reverse();
        int i = 0;
        //noinspection StatementWithEmptyBody
        for (; sb.length() > 0 && Character.isAlphabetic(sb.charAt(i)); i++) ;
        return sb.delete(i, sb.length()).reverse().toString();
    }
}
