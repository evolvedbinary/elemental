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

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import org.exist.xquery.XPathException;

import java.math.BigInteger;
import java.util.*;

import static org.exist.util.StringUtil.capitalize;

/**
 * Format numbers according to rules 6/7/8 (formatting as words)
 * <a href="https://www.w3.org/TR/xpath-functions-31/#formatting-integers">format-integer</a>
 * <p>
 * Uses <a href="https://unicode-org.github.io/icu/userguide/icu4j/">ICU4J</a> to format the numbers
 * <p>
 * Ask ICU4J which "spellouts" are available for a language, guided by the format modifier
 * Use the closest spellout available.
 */
public class WordPicture extends IntegerPicture {

    static final String SPELLOUT_PREFIX_CARDINAL = "%spellout-cardinal";
    static final String SPELLOUT_PREFIX_ORDINAL = "%spellout-ordinal";
    static final List<String> SPELLOUT_EXTENSIONS = Arrays.asList("-feminine", "-masculine", "-neuter", "-native", "-common");

    /**
     * Common variations for formatters used by other implementations of format-integer.
     * <p>
     * Formatting German text, a user might ask for format modifier : "o(-er)"
     * This means they want ordinal text, using a formatter with -er endings.
     * There is a formatter
     */
    static final Map<String, Map<String, String>> shorthands = new HashMap<>();

    static {
        final Map<String, String> de = new HashMap<>();
        de.put("er", "r");
        de.put("es", "s");
        de.put("en", "n");
        WordPicture.shorthands.put("de", de);
        final Map<String, String> it = new HashMap<>();
        it.put("o", "masculine");
        it.put("a", "feminine");
        WordPicture.shorthands.put("it", it);
    }

    final CaseAndCaps capitalization;
    final FormatModifier formatModifier;

    WordPicture(final CaseAndCaps capitalization, final FormatModifier formatModifier) {
        this.capitalization = capitalization;
        this.formatModifier = formatModifier;
    }

    /**
     * Search for all the spellouts satisfying a locale and a prefix
     *
     * @param locale     search for spellouts in this locale
     * @param rulePrefix search for spellouts starting with this prefix
     * @return the set of spellouts found
     */
    static Set<String> getPrefixedSpelloutRules(final Locale locale, final String rulePrefix) {
        final RuleBasedNumberFormat ruleBasedNumberFormat = new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.SPELLOUT);

        final Set<String> spelloutRuleSet = new HashSet<>();
        for (final String ruleSetName : ruleBasedNumberFormat.getRuleSetNames()) {
            if (ruleSetName.startsWith(rulePrefix)) {
                spelloutRuleSet.add(ruleSetName);
            }
        }

        return spelloutRuleSet;
    }

    /**
     * Pick the best match spellout for a language and format modifier
     *
     * @param locale         to pick a spellout for
     * @param formatModifier what sort of spellout ? ordinal or cardinal ? May contain name or suffix of the spellout required
     * @return our best guess at an appropriate spellout
     */
    static String getBestSpellout(final Locale locale, final FormatModifier formatModifier) {

        String spelloutPrefix = null;
        if (formatModifier.numbering == FormatModifier.Numbering.CARDINAL) spelloutPrefix = WordPicture.SPELLOUT_PREFIX_CARDINAL;
        if (formatModifier.numbering == FormatModifier.Numbering.ORDINAL) spelloutPrefix = WordPicture.SPELLOUT_PREFIX_ORDINAL;

        String spellout = WordPicture.getSpellout(locale, formatModifier, spelloutPrefix);
        if (spellout == null && formatModifier.numbering == FormatModifier.Numbering.ORDINAL) {
            // Back off to cardinal if we can't get an ordinal spellout
            spellout = WordPicture.getSpellout(locale, formatModifier, WordPicture.SPELLOUT_PREFIX_CARDINAL);
        }
        return spellout;
    }

    static String mapVariationShorthand(final String language, final String variation) {
        if (variation == null) {
            return null;
        }
        final String trimmedVariation = variation.replace("-", "");
        final Map<String, String> languageMap = WordPicture.shorthands.get(language);
        if (languageMap == null) {
            //nothing to map
            return variation;
        }
        final String mappedVariation = languageMap.get(trimmedVariation);
        if (mappedVariation == null) {
            //not a known shorthand
            return variation;
        }
        if (variation.startsWith("-")) {
            return "-" + mappedVariation;
        } else {
            return mappedVariation;
        }
    }

    /**
     * Pick the best match spellout for a language
     *
     * @param locale           to pick a spellout for
     * @param formatModifier   ordinal or cardinal ? Any hints at the spellout required ?
     * @param prefixAndDefault there is always a spellout with no suffix which can be used to fall back
     * @return our best guess at an appropriate spellout
     */
    static String getSpellout(final Locale locale, final FormatModifier formatModifier, final String prefixAndDefault) {

        // All the possibilities
        final Set<String> spelloutRuleSet = WordPicture.getPrefixedSpelloutRules(locale, prefixAndDefault);

        // Special case a prefix we know about into the standard ICU4J ones
        final String variation = WordPicture.mapVariationShorthand(locale.getLanguage(), formatModifier.variation);

        // Match the variation with the possibilities
        if (variation != null) {
            final String variantSpelloutRule;
            if (variation.startsWith("-")) {
                variantSpelloutRule = prefixAndDefault + variation;
            } else {
                variantSpelloutRule = prefixAndDefault + "-" + variation;
            }
            if (spelloutRuleSet.contains(variantSpelloutRule)) {
                return variantSpelloutRule;
            } else if (spelloutRuleSet.contains(variation)) {
                return variation;
            }
        }

        // None of the variations matched
        if (spelloutRuleSet.contains(prefixAndDefault)) {
            return prefixAndDefault;
        }

        // The default was not available - try standard variations in order
        for (final String extension : WordPicture.SPELLOUT_EXTENSIONS) {
            if (spelloutRuleSet.contains(prefixAndDefault + extension)) {
                return prefixAndDefault + extension;
            }
        }
        return null;
    }

    enum CaseAndCaps {
        UPPER, LOWER, CAPITALIZED;

        /**
         * Format an integer and then capitalize it correctly
         *
         * @param value          to format
         * @param locale         to format in
         * @param formatModifier guidance on how to format
         * @return the formatted and converted integer as a string
         */
        String formatAndConvert(final int value, final Locale locale, final FormatModifier formatModifier) {

            final String spelloutRule = WordPicture.getBestSpellout(locale, formatModifier);

            final MessageFormat ruleBasedMessageFormatFormat = new MessageFormat("{0,spellout," + spelloutRule + "}", locale);
            final String formatted = ruleBasedMessageFormatFormat.format(new Object[]{value});

            String result = null;
            switch (this) {
                case UPPER:
                    result = formatted.toUpperCase(locale);
                    break;
                case LOWER:
                    result = formatted;
                    break;
                case CAPITALIZED:
                    result = capitalize(formatted);
                    break;
            }
            return result;
        }
    }

    @Override
    public String formatInteger(final BigInteger bigInteger, final Locale locale) throws XPathException {
        //spec says out of range should be formatted by "1"
        if (bigInteger.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 || bigInteger.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            return IntegerPicture.defaultPictureWithModifier(new FormatModifier("")).formatInteger(bigInteger, locale);
        }

        final BigInteger absInteger = bigInteger.abs();
        String prefix = "";
        if (absInteger.compareTo(bigInteger) != 0) {
            prefix = "-";
        }
        return prefix + capitalization.formatAndConvert(absInteger.intValue(), locale, formatModifier);
    }
}
