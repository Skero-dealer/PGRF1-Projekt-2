package graphics.filler;

import graphics.Raster;
import model.Line;
import model.Point;
import model.Polygon;

import java.util.ArrayList;
import java.util.List;

public class CreeperFaceFiller implements PatternFill {

    private final Raster raster;
    private int green = 0x0ffc03;
    private int black = 0x000000;

    // Fun fact: pokud kliknete a highlightnete jednu "black", tak se vám ten vzor ukáže už tady ;)
    private int[][] thePattern = {
            {green, green ,green, green, green, green, green, green, green ,green},
            {green, black, black, green, green, green, green, black, black, green},
            {green, black, black, green, green, green, green, black, black, green},
            {green, green, green, black, black, black, black, green, green, green},
            {green, green, green, black, black, black, black, green, green, green},
            {green, black, black, black, black, black, black, black, black, green},
            {green, black, black, black, black, black, black, black, black, green},
            {green, black, black, green, green, green, green, black, black, green},
            {green, black, black, green, green, green, green, black, black, green},
            {green, green, green, green, green, green, green, green, green, green},
    };
    private int patternSizeI = thePattern.length;
    private int patternSizeJ = thePattern[0].length;

    public CreeperFaceFiller(Raster raster) {
        this.raster = raster;
    }

    @Override
    public void fill(Polygon polygon, int fillColor, int fillColor2) {
        List<Line> edges = new ArrayList<>(polygon.getLines());
        if (edges.size() < 2) {
            return;
        }

        // Uzavření polygonu
        Line last = edges.getLast();
        Line first = edges.getFirst();
        edges.add(new Line(last.getPoint2(), first.getPoint1(), last.getColor()));

        // Orientace hran & určení rozsahu
        int maxY = 0;
        int minY = raster.getHeight();
        for (int i = 0; i < edges.size(); i++) {
            Line edge = edges.get(i);
            Point p1 = edge.getPoint1();
            Point p2 = edge.getPoint2();

            if (p1.getY() > p2.getY()) {
                Point temp = p1;
                p1 = p2;
                p2 = temp;
                edges.set(i, new Line(p1, p2, fillColor));
            }

            minY = Math.min(minY, p1.getY());
            maxY = Math.max(maxY, p2.getY());
        }

        // Průsečíky hran se středem řádku
        List<Double> intersections = new ArrayList<>();
        for (int y = Math.max(0, minY); y < Math.min(raster.getHeight(), maxY); y++) {
            intersections.clear();
            double scanY = y + 0.5;
            // Výpočet průsečíků
            for (Line edge : edges) {
                Point p1 = edge.getPoint1();
                Point p2 = edge.getPoint2();
                // Odstranění rovných čár & spodních bodů
                if (y < p1.getY() || y >= p2.getY()) {
                    continue;
                }
                // Souřadnice (průsečíky) x
                Double x = p1.getX() + (scanY - p1.getY()) * (p2.getX() - p1.getX()) / (p2.getY() - p1.getY());
                intersections.add(x);
            }

            // Seřazení & vyplnění pixelů
            intersections.sort(null);
            for (int i = 0; i+ 1 < intersections.size(); i = i + 2) {
                // Dopočítání x souřadnice
                int from = Math.max(0, (int) Math.ceil(intersections.get(i) - 0.5));
                int to = Math.min(raster.getWidth(), (int) Math.ceil(intersections.get(i+1) - 0.5));

                // Projdeme od "from" do "to" a obarvíme pixely
                for (int x = from; x < to; x++) {
                    int finalColor = thePattern[y % patternSizeI][x % patternSizeJ];

                    raster.setPixel(x, y, finalColor);
                }
            }
        }
    }
}
