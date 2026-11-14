package graphics.filler;

import graphics.Raster;

import java.util.ArrayDeque;
import java.util.Deque;

public class StackSeedFiller implements SeedFiller{

    private final Raster raster;

    public StackSeedFiller(Raster raster) {
        this.raster = raster;
    }

    @Override
    public boolean fill(int x, int y, int backgroundColor, int fillColor) {
        // Tady ta if podmínka řeší "Jednak omezení barvou pozadí a jednak barvou hranice."
        // První podmínka řeší barvu hranice.
        // Druhá podmínka řeší jestli je pixel mimo canvas.
        // Třetí podmínka řeší barvu pozadí.
        // Pokud to tímto není myšleno, tak by se tu dal 5. parametr — int borderColor a pak bychom do podmínky jen přídali
        // "|| backgroundColor == borderColor)"
        if (backgroundColor == fillColor || raster.getPixel(x, y) == null || raster.getPixel(x, y) != backgroundColor) {
            return  false;
        }

        int width = raster.getWidth();
        Deque<Integer> pixels = new ArrayDeque<>();
        enqueue(x, y, width, backgroundColor, fillColor, pixels);

        while(!pixels.isEmpty()) {
            int index = pixels.removeLast();
            int newX = index % width;
            int newY = index / width;

            enqueue(newX - 1, newY, width, backgroundColor, fillColor, pixels);
            enqueue(newX + 1, newY, width, backgroundColor, fillColor, pixels);
            enqueue(newX , newY- 1, width, backgroundColor, fillColor, pixels);
            enqueue(newX, newY + 1, width, backgroundColor, fillColor, pixels);
        }
        return true;
    }

    private void enqueue(int x, int y, int width, int backgroundColor, int fillColor, Deque<Integer> pixels) {
        Integer color = raster.getPixel(x, y);
        if (color != null && color == backgroundColor) {
            raster.setPixel(x, y, fillColor);
            pixels.addLast(y * width + x);
        }
    }
}
