package com.airmusic.player.sonnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SonnetLayoutTest {

    @Test
    public void portraitCjkWordUsesAnUprightColumn() {
        assertTrue(SonnetLayout.shouldUseVerticalFlow(false, true, true));
        assertEquals(0f, SonnetLayout.verticalFlowRotation(false, true, true, 2), 0f);
    }

    @Test
    public void portraitLatinWordIsRotatedExactlyOnce() {
        assertFalse(SonnetLayout.shouldUseVerticalFlow(true, true, false));
        assertEquals((float) (Math.PI / 2),
                SonnetLayout.verticalFlowRotation(true, true, false, 5), 0f);
    }

    @Test
    public void requestedVerticalLatinWordIsNotRotatedTwice() {
        assertFalse(SonnetLayout.shouldUseVerticalFlow(true, false, false));
        assertEquals((float) (Math.PI / 2),
                SonnetLayout.verticalFlowRotation(true, false, false, 5), 0f);
    }

    @Test
    public void horizontalLatinWordKeepsItsNormalReadingDirection() {
        assertFalse(SonnetLayout.shouldUseVerticalFlow(false, false, false));
        assertEquals(0f,
                SonnetLayout.verticalFlowRotation(false, false, false, 5), 0f);
    }
}
