package com.java.myapplication;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScreenClassificationTest {
    @Test public void tabletDimensionsReplacePhoneSizeAndAspectFlags() {
        assertEquals(0x10000014, ScreenClassification.screenLayout(0x22, 800, 1280));
        assertEquals(0x10000014, ScreenClassification.screenLayout(0x22, 1280, 800));
        assertEquals(0x10000014, ScreenClassification.screenLayout(0x22, 900, 1440));
    }

    @Test public void phoneDimensionsReplaceTabletSizeAndAspectFlags() {
        assertEquals(0x10000022, ScreenClassification.screenLayout(0x10000014, 411, 914));
        assertEquals(0x11, ScreenClassification.screenLayout(0x10000014, 320, 426));
    }

    @Test public void classificationKeepsLayoutDirectionRoundAndOtherFlags() {
        assertEquals(0x10000294, ScreenClassification.screenLayout(0x2a2, 800, 1280));
    }

    @Test public void androidSizeBoundariesAreRespected() {
        assertEquals(0x10000013, ScreenClassification.screenLayout(0, 600, 960));
        assertEquals(0x10000014, ScreenClassification.screenLayout(0, 720, 960));
        assertEquals(0x10000013, ScreenClassification.screenLayout(0, 480, 640));
        assertEquals(0x10000022, ScreenClassification.screenLayout(0, 479, 800));
    }

    @Test public void tabletDetectionUsesDpRatherThanPixelResolution() {
        assertTrue(ScreenClassification.isTablet(1600, 2560, 320));
        assertTrue(ScreenClassification.isTablet(1200, 1920, 240));
        assertFalse(ScreenClassification.isTablet(1440, 3200, 560));
        assertFalse(ScreenClassification.isTablet(1080, 2400, 0));
    }

    @Test public void tabletPropertyRemovesOnlyHandheldClassTokens() {
        assertEquals("tablet", ScreenClassification.tabletCharacteristics("default"));
        assertEquals("tablet,nosdcard", ScreenClassification.tabletCharacteristics("phone,nosdcard"));
        assertEquals("tablet,nosdcard", ScreenClassification.tabletCharacteristics("tablet,nosdcard"));
        assertEquals("tablet", ScreenClassification.tabletCharacteristics(""));
    }
}
