package graphics.clipping;

import model.Point;
import model.Polygon;
import model.Line;

public class CohenSutherlandClipper implements LineClipper {

    // Bitové konstanty pro oblasti (Outcodes)
    private static final int INSIDE = 0; // 0000
    private static final int LEFT   = 1; // 0001
    private static final int RIGHT  = 2; // 0010
    private static final int BOTTOM = 4; // 0100
    private static final int TOP    = 8; // 1000

    // Pomocná metoda: c = kód(bod)
    // Tady prostě jen určujeme jestli je nahoře, dole, vlevo nebo vpravo pomocí bitové operace OR
    // Třeba 0010 OR 0100 = 0110
    // Ze začátku tudíž předpokládáme že je bod uvnitř (0000)
    private int computeCode(double x, double y, int xmin, int ymin, int xmax, int ymax) {
        int code = INSIDE;
        if (x < xmin) {
            code |= LEFT;
        } else if (x > xmax) {
            code |= RIGHT;
        }
        if (y < ymin) {
            code |= BOTTOM;
        } else if (y > ymax) {
            code |= TOP;
        }
        return code;
    }

    @Override
    public Line clip(Line targetLine, Polygon boundary) {
        int lineColor = targetLine.getColor();

        // Určíme si hranici okna (boundary/ořezávacího polygonu)
        int xmax = Integer.MIN_VALUE;
        int xmin = Integer.MAX_VALUE;
        int ymax = Integer.MIN_VALUE;
        int ymin = Integer.MAX_VALUE;

        for (Line boundaryLine : boundary.getLines()) {
            xmax = Math.max(xmax, Math.max(boundaryLine.getPoint1().getX(), boundaryLine.getPoint2().getX()));
            ymax = Math.max(ymax, Math.max(boundaryLine.getPoint1().getY(), boundaryLine.getPoint2().getY()));
            xmin = Math.min(xmin, Math.min(boundaryLine.getPoint1().getX(), boundaryLine.getPoint2().getX()));
            ymin = Math.min(ymin, Math.min(boundaryLine.getPoint1().getY(), boundaryLine.getPoint2().getY()));
        }

        // Použijeme double, aby při dělení nedocházelo k celočíselnému zaokrouhlování
        double x0 = targetLine.getPoint1().getX();
        double y0 = targetLine.getPoint1().getY();
        double x1 = targetLine.getPoint2().getX();
        double y1 = targetLine.getPoint2().getY();

        // Opakujeme cyklus dokud nám z toho něco nevypadne
        // Minimálně se provede 4x protože zkontrolujeme každý bod alespoň 2x
        while (true) {
            // cA = kód(A); cB = kód(B)
            // Určíme si kódy pro naše body
            int codeFirstPoint = computeCode(x0, y0, xmin, ymin, xmax, ymax);
            int codeLastPoint = computeCode(x1, y1, xmin, ymin, xmax, ymax);

            // pokud (cA | cB) == 0: přijmi úsečku A-B; konec
            // Tady to znamená že oba body jsou 0000, k tomuto výsledku se nevyhnutelně dostaneme když budeme
            // body posouvat k INSIDE, což vlastně je přesně co děláme každý cyklus
            if ((codeFirstPoint | codeLastPoint) == 0) {
                Point pA = new Point((int) Math.round(x0), (int) Math.round(y0));
                Point pB = new Point((int) Math.round(x1), (int) Math.round(y1));
                return new Line(pA, pB, lineColor);
            }

            // pokud (cA & cB) != 0: zahoď úsečku; konec
            if ((codeFirstPoint & codeLastPoint) != 0) {
                return null; // Úsečka je zcela mimo okno
            }

            // c = cA != 0 ? cA : cB
            // Tady si prostě jen určíme kterou z těch bodů budeme řešit.
            // Tkaže když náš poslední bod už je v INSIDE, tak prostě řešíme první bod.
            // Je to jen abychom v příštím kroce řešili jen jeden bod a ten druhý prostě nechali na další cyklus.
            // proto cykluse se vykoná minimálně 4x.
            int c = codeLastPoint;
            if (codeFirstPoint != 0) {
                c = codeFirstPoint;
            }

            double x = 0;
            double y = 0;

            // podle nastaveného bitu v c: najdi průsečík s hranou okna
            // Upřímně tady to je prostě tuna matematiky, prosím nikdy se mě neptejte jak tohle funguje
            if ((c & TOP) != 0) {           // Bod je nad oknem
                x = x0 + (x1 - x0) * (ymax - y0) / (y1 - y0);
                y = ymax;
            } else if ((c & BOTTOM) != 0) { // Bod je pod oknem
                x = x0 + (x1 - x0) * (ymin - y0) / (y1 - y0);
                y = ymin;
            } else if ((c & RIGHT) != 0) {  // Bod je vpravo od okna
                y = y0 + (y1 - y0) * (xmax - x0) / (x1 - x0);
                x = xmax;
            } else if ((c & LEFT) != 0) {   // Bod je vlevo od okna
                y = y0 + (y1 - y0) * (xmin - x0) / (x1 - x0);
                x = xmin;
            }

            // nahraď vnější bod průsečíkem
            if (c == codeFirstPoint) {
                x0 = x;
                y0 = y;
            } else {
                x1 = x;
                y1 = y;
            }
        }
    }
}