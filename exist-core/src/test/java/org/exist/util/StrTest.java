package org.exist.util;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.exist.xmldb.concurrent.DBUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmldb.api.base.XMLDBException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.lang.Math.signum;
import static org.assertj.core.api.Assertions.assertThat;

public class StrTest {

    @BeforeEach
    public void reset() {
        Str.invalidateCache();
    }

    @Test
    public void testNullOrEmptyStr() {
        assertThat(Str.of(null)).isEqualTo(null);
        assertThat(Str.of("").toString()).isEqualTo("");

        assertThat(Str.of("")).isNotEqualTo(Str.of("1"));
        assertThat(Str.of("1")).isNotEqualTo(Str.of(""));
    }

    @Test
    public void testCache() {
        CacheStats before = Str.getCacheStats();
        Random random = new Random();
        for (int i = 0; i < 1000000; i++) {
            final int rand = random.nextInt(1000);
            Str s = Str.of(String.format("%4d", rand));
            assertThat(s.toString().getBytes(StandardCharsets.UTF_8)).isEqualTo(String.format("%4d", rand).getBytes(StandardCharsets.UTF_8));
            assertThat(s.length()).isEqualTo(4);
        }
        CacheStats stats = Str.getCacheStats().minus(before);
        assertThat(stats.evictionCount()).isEqualTo(0);
        assertThat(stats.hitCount()).isEqualTo(999000);
    }

    @Test
    public void testOrder() {
        CacheStats before = Str.getCacheStats();
        for (int i = 0; i < 99; i++) {
            Str si0 = Str.of(String.format("%3d",i));
            Str si1 = Str.of(String.format("%3d",i+1));
            assertThat(si0.compareTo(si1)).as("[%s] < [%s]", si0, si1).isLessThan(0);
        }
        for (int i = 1; i < 100; i++) {
            Str si0 = Str.of(String.format("%3d",i));
            Str si1 = Str.of(String.format("%3d",i-1));
            assertThat(si0.compareTo(si1)).as("[%s] < [%s]", si0, si1).isGreaterThan(0);
        }

        CacheStats stats = Str.getCacheStats().minus(before);
        assertThat(stats.evictionCount()).isEqualTo(0);
        assertThat(stats.hitCount()).isEqualTo(296);
    }

    @Test
    public void testWords() throws XMLDBException {
        final Map<Str, String> forward = new HashMap<>();
        final Map<String, Str> reverse = new HashMap<>();
        String[] wordList = DBUtils.wordList();
        for (String word : wordList) {
            assertThat(Str.of(word).toString()).isEqualTo(word);
            Str str = Str.of(word);
            forward.put(str, word);
            reverse.put(word, str);
        }
        assertThat(forward.size()).isEqualTo(wordList.length);
        assertThat(reverse.size()).isEqualTo(wordList.length);

        final Map<Str, String> forward2 = new HashMap<>(forward);
        for (Map.Entry<String,Str> entry : reverse.entrySet()) {
            assertThat(forward2.containsKey(entry.getValue())).isTrue();
            forward2.remove(entry.getValue());
        }
        assertThat(forward2.isEmpty()).isTrue();

        final Map<String, Str> reverse2 = new HashMap<>(reverse);

        for (Map.Entry<Str,String> entry : forward.entrySet()) {
            assertThat(reverse2.containsKey(entry.getValue())).isTrue();
            reverse2.remove(entry.getValue());
        }
        assertThat(reverse2.isEmpty()).isTrue();
    }

    @Test
    public void comparableIdentity() throws XMLDBException {
        String[] wordList = DBUtils.wordList();
        for (String word : wordList) {
            float compareStrs = signum(Str.of(word).compareTo(Str.of(word)));
            assertThat(compareStrs).as("compare %s to self not 0",word,compareStrs).isEqualTo(0);
        }
    }

    @Test
    public void comparableForwardSimple() throws XMLDBException {
        assertThat(signum(Str.of("word0Fwd").compareTo(Str.of("Aberaeron")))).isEqualTo(1);
    }

    @Test
    public void comparableForward() throws XMLDBException {
        String[] wordList = DBUtils.wordList();
        String prev = "word0Fwd";
        for (String word : wordList) {
            float compareStrings = signum(prev.compareTo(word));
            float compareStrs = signum(Str.of(prev).compareTo(Str.of(word)));
            assertThat(compareStrs).as("compare %s to %s by string %f but by str %f",prev,word,compareStrings,compareStrs).isEqualTo(compareStrings);
            prev = word;
        }
    }

    @Test
    public void comparableReverse() throws XMLDBException {
        String[] wordList = DBUtils.wordList();
        String prev = "word0Rev";
        for (String word : wordList) {
            float compareStrings = signum(word.compareTo(prev));
            float compareStrs = signum(Str.of(word).compareTo(Str.of(prev)));
            assertThat(compareStrs).as("compare %s to %s by string %f but by str %f",prev,word,compareStrings,compareStrs).isEqualTo(compareStrings);
            prev = word;
        }
    }

    /**
     * Test some assumptions about the fingerprinting algo
     */
    @Test
    public void fingerprint() {
        assertThat(Str.fingerprint("")).isEqualTo(0L);
        assertThat(Str.fingerprint("\0")).isEqualTo(1L << 48);
        assertThat(Str.fingerprint("\0\0")).isEqualTo(2L << 48);
        assertThat(Str.fingerprint("\0\0\0\0\0\0")).isEqualTo(6L << 48);
        assertThat(Str.fingerprint("\0\0\0\0\0\0\0")).isEqualTo(7L << 48);
        assertThat(Str.fingerprint("\0\0\0\0\0\0\0\0")).isEqualTo(8L << 48);

        assertThat(Str.fingerprint(new String(new char[]{0x100}))).isEqualTo(1L << 48 | 1L << 56);
        assertThat(Str.fingerprint(new String(new char[]{0x100,0x200}))).isEqualTo(2L << 48 | 3L << 56);
        assertThat(Str.fingerprint(new String(new char[]{0x100,0x200,0x400}))).isEqualTo(3L << 48 | 7L << 56);
        assertThat(Str.fingerprint(new String(new char[]{0x100,0x200,0x400,0x400}))).isEqualTo(4L << 48 | 7L << 56);
    }
}
