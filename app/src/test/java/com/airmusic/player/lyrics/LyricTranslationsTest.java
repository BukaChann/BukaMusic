package com.airmusic.player.lyrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class LyricTranslationsTest {

    @Test
    public void separatesInlineBilingualTextFromDynamicTypography() {
        Lyrics parsed = LrcParser.parse(
                "[00:01.00]Life blooms like a flower 生命如花绽放\n"
                        + "[00:04.00]Far away or by the road 无论远方还是路旁",
                8_000L
        );

        Lyrics resolved = LyricTranslations.resolve(parsed);

        assertEquals(2, resolved.lines.size());
        assertEquals("Life blooms like a flower", resolved.lines.get(0).text);
        assertEquals("生命如花绽放", resolved.lines.get(0).translation);
        assertEquals("Far away or by the road", resolved.lines.get(1).text);
        assertEquals("无论远方还是路旁", resolved.lines.get(1).translation);
    }

    @Test
    public void pairsSeparateTimestampedTranslationWithoutDuplicatingLine() {
        Lyrics parsed = new Lyrics(
                java.util.Arrays.asList(
                        new LyricLine(1_000L, 4_000L, "Say my name"),
                        new LyricLine(1_180L, 4_180L, "呼唤我的名字")
                ),
                true
        );

        Lyrics resolved = LyricTranslations.resolve(parsed);

        assertEquals(1, resolved.lines.size());
        assertEquals("Say my name", resolved.lines.get(0).text);
        assertNotNull(resolved.lines.get(0).translation);
        assertEquals("呼唤我的名字", resolved.lines.get(0).translation);
    }
}
