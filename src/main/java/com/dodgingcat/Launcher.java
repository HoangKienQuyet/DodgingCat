package com.dodgingcat;

import com.dodgingcat.app.DodgingCatApp;
import com.dodgingcat.app.Java2DApp;

/**
 * Core Java2D is the native-resolution desktop backend. The previous FX backend
 * remains available for comparison with -Ddodgingcat.renderer=javafx.
 */
public class Launcher {
    public static void main(String[] args) throws Exception {
        com.dodgingcat.app.RenderSurface.configureRuntime();
        String renderer = System.getProperty("dodgingcat.renderer", "java2d");
        switch (renderer.toLowerCase(java.util.Locale.ROOT)) {
            case "java2d" -> Java2DApp.main(args);
            case "javafx" -> DodgingCatApp.main(args);
            default -> throw new IllegalArgumentException("Unknown renderer: " + renderer);
        }
    }
}
