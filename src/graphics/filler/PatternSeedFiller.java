package graphics.filler;

import graphics.Raster;

public class PatternSeedFiller implements SeedFiller {

    private final Raster raster;

    public PatternSeedFiller(Raster raster) {
        this.raster = raster;
    }

    @Override
    public boolean fill(int x1, int y1, int targetColor, int fillColor) {
        if (targetColor == fillColor || raster.getPixel(x1, y1) == null || raster.getPixel(x1, y1) != targetColor) {
            return false;
        }

        fillRegion(x1, y1, targetColor, fillColor);
        return true;
    }

    private void fillRegion(int x, int y, int targetColor, int fillColor) {
        Integer color = raster.getPixel(x, y);
        if (color == null || color != targetColor) {
            return;
        }

        int finalColor = fillColor;
        if (x % 8 < 4 && y % 8 < 4) {
            finalColor = targetColor;
        }

        raster.setPixel(x, y, finalColor);
        fillRegion(x - 1, y, targetColor, fillColor);
        fillRegion(x + 1, y, targetColor, fillColor);
        fillRegion(x, y - 1, targetColor, fillColor);
        fillRegion(x, y + 1, targetColor, fillColor);

        fillRegion(x - 1, y - 1, targetColor, fillColor);
        fillRegion(x - 1, y + 1, targetColor, fillColor);
        fillRegion(x + 1, y - 1, targetColor, fillColor);
        fillRegion(x + 1, y + 1, targetColor, fillColor);
    }

}
