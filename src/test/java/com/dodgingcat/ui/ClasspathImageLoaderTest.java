package com.dodgingcat.ui;

import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClasspathImageLoaderTest {

    @Test
    void loadsTheCozyRoomBackgroundAtItsNativeWorldDimensions() {
        Image room = new ClasspathImageLoader().loadPng(PixelAssetPaths.ROOM_BACKGROUND)
                .orElseThrow();

        assertEquals(900, room.getWidth());
        assertEquals(540, room.getHeight());
    }
}
