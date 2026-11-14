package graphics.filler;

import graphics.Raster;
import model.Line;
import model.Point;
import model.Polygon;

import java.util.ArrayList;
import java.util.List;

public class ModernArtFiller implements PatternFill {

    private final Raster raster;
    private int newFillColor = 0xff00f2;
    private int newFillColor2 = 0x00eaff;

    public ModernArtFiller(Raster raster) {
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
        int maxHeight = Math.min(raster.getHeight(), maxY);
        for (int y = Math.max(0, minY); y < maxHeight; y++) {
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
                    int targetColor = (fillColor/(maxHeight/y)) + (fillColor2/(from+1/to+1));
                    raster.setPixel(x, y, targetColor);
                }
            }
        }
    }
}
