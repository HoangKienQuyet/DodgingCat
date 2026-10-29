import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class MapConfig {
    public String name;
    public String bgPath;
    public String platformPath;
    public String[] hazardPaths;

    public BufferedImage bgImage;
    public BufferedImage platformImage;
    public List<BufferedImage> hazardImages = new ArrayList<>();

    public MapConfig(String name, String bgPath, String platformPath, String[] hazardPaths) {
        this.name = name;
        this.bgPath = bgPath;
        this.platformPath = platformPath;
        this.hazardPaths = hazardPaths;
    }

    public void loadAssets() {
        this.bgImage = AssetLoader.loadImage(bgPath);
        this.platformImage = AssetLoader.loadImage(platformPath);
        for (String path : hazardPaths) {
            BufferedImage img = AssetLoader.loadImage(path);
            if (img != null) {
                hazardImages.add(img);
            }
        }
    }
}