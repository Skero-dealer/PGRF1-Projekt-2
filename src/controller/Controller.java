package controller;
/*
        .___  ___.   ______   .__   __.  _______ ____    ____    ____    __    ____  __   ________       ___      .______       _______       _______      ___      .__   __.   _______
        |   \/   |  /  __  \  |  \ |  | |   ____|\   \  /   /    \   \  /  \  /   / |  | |       /      /   \     |   _  \     |       \     /  _____|    /   \     |  \ |  |  /  _____|
        |  \  /  | |  |  |  | |   \|  | |  |__    \   \/   /      \   \/    \/   /  |  | `---/  /      /  ^  \    |  |_)  |    |  .--.  |   |  |  __     /  ^  \    |   \|  | |  |  __
        |  |\/|  | |  |  |  | |  . `  | |   __|    \_    _/        \            /   |  |    /  /      /  /_\  \   |      /     |  |  |  |   |  | |_ |   /  /_\  \   |  . `  | |  | |_ |
        |  |  |  | |  `--'  | |  |\   | |  |____     |  |           \    /\    /    |  |   /  /----. /  _____  \  |  |\  \----.|  '--'  |   |  |__| |  /  _____  \  |  |\   | |  |__| |
        |__|  |__|  \______/  |__| \__| |_______|    |__|            \__/  \__/     |__|  /________|/__/     \__\ | _| `._____||_______/     \______| /__/     \__\ |__| \__|  \______|
*/

import graphics.clipping.CohenSutherlandClipper;
import graphics.clipping.LineClipper;
import graphics.filler.*;
import graphics.clipping.PolygonClipper;
import graphics.clipping.SutherlandHodgmanClipper;
import graphics.filler.StackSeedFiller;
import graphics.rasterizer.PolygonOutlineRasterizer;
import graphics.rasterizer.PolygonRasterizer;
import graphics.rasterizer.XiaolinWuLineRasterizer;
import model.Line;
import model.Point;
import model.Polygon;
import view.Canvas;
import view.gui.Toolbar;

import java.awt.Color;
import java.awt.event.*;
import java.util.List;

import javax.swing.AbstractAction;

import static java.awt.event.KeyEvent.VK_C;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.KeyStroke.getKeyStroke;

public class Controller {

    private final Canvas canvas;
    private final Toolbar toolbar;

    private Line previewLine;
    private final Polygon polygon;
    private final PolygonClipper clipperPolygon;
    private final LineClipper clipperLine;
    private int clippingMode = 1; // 1 = SutherlandHodgman; 2 = Cohen-Sutherland
    private Polygon clippingTriangle;
    private final PolygonRasterizer polygonRasterizer;

    private Point seedFillPoint;
    private int seedFillMode = 1; // 1 = No point; 2 = Choosing point; 3 = Point chosen
    private final SeedFiller seedFiller;
    private final ScanlineFiller scanlineFiller;

    private static final int LINE_COLOR = Color.WHITE.getRGB();
    private static final int PREVIEW_COLOR = Color.RED.getRGB();
    private static final int ORIGINAL_COLOR = new Color(0x9099A3).getRGB();
    private static final int CLIPPED_COLOR = new Color(0xA4F2DF).getRGB();
    private static final int CLIPPING_TRIANGLE_COLOR = new Color(0xE6B85C).getRGB();
    private static final int FILL_COLOR = new Color(0x58BFA9).getRGB();

    // Tento tvar se mi zjevil ve snu (Ne, ale bylo by to vtipné)
    private static final List<Point> HODGMAN_CLIPPING_TRIANGLE_VERTICES = List.of(
            new Point(250, 200),
            new Point(375, 100),
            new Point(500, 200),
            new Point(625, 300),
            new Point(500, 400),
            new Point(375, 500),
            new Point(250, 400)
    );

    private static final List<Point> COHEN_CLIPPING_POLYGON_VERTICES = List.of(
            new Point(250, 200),
            new Point(500, 200),
            new Point(500, 400),
            new Point(250, 400)
    );

    public Controller(Canvas canvas, Toolbar toolbar) {
        this.canvas = canvas;
        this.toolbar = toolbar;
        this.polygon = new Polygon();
        // Fillers
        this.scanlineFiller = new ScanlineFiller(canvas.getRaster());
        this.seedFiller = new StackSeedFiller(canvas.getRaster());
        // Rasterizers
        this.polygonRasterizer = new PolygonOutlineRasterizer(new XiaolinWuLineRasterizer(canvas.getRaster()));
        // Clipping
        this.clipperPolygon = new SutherlandHodgmanClipper();
        this.clipperLine = new CohenSutherlandClipper();
        this.clippingTriangle = createPolygon(HODGMAN_CLIPPING_TRIANGLE_VERTICES, CLIPPING_TRIANGLE_COLOR);
    }

    public void init() {
        toolbar.addClearListener(_ -> clearDrawing());
        toolbar.addSeedFillListener(_ -> cycleSeedFillMode());
        toolbar.addClippingListener(_ -> cycleClippingMode());
        canvas.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(getKeyStroke(VK_C, 0), "clearCanvas");
        canvas.getActionMap().put("clearCanvas", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                clearDrawing();
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    if (seedFillMode == 2) {
                        fillAt(event);
                        cycleSeedFillMode();
                    } else {
                        if (clippingMode == 2) {
                            polygon.clear();
                        }

                        startLine(event);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                if (previewLine == null) {
                    return;
                }
                Point startPoint = previewLine.getPoint1();
                Point endPoint = getEndLinePoint(event, startPoint);

                polygon.addLine(new Line(startPoint, endPoint, LINE_COLOR));
                previewLine = null;
                render();
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                if (previewLine != null) {
                    Point startPoint = previewLine.getPoint1();
                    Point endPoint = getEndLinePoint(event, startPoint);

                    previewLine = new Line(startPoint, endPoint, PREVIEW_COLOR);
                    render();
                }
            }
        });

        render();
    }

    private void render() {
        canvas.clear();
        if (clippingMode == 1) {
            // Clip polygon
            Polygon subject = withPreviewLine(polygon, previewLine);
            Polygon clipped;
            try {
                clipped = clipperPolygon.clip(subject, clippingTriangle);
            } catch (UnsupportedOperationException unfinishedClipper) {
                clipped = null;
            }
            if (clipped != null) {
                scanlineFiller.fill(clipped, FILL_COLOR);
            }

            polygonRasterizer.rasterize(withColor(polygon, ORIGINAL_COLOR), previewLine);
            if (clipped != null) {
                polygonRasterizer.rasterize(withColor(clipped, CLIPPED_COLOR), null);
            }
        } else if (clippingMode == 2) {

            // Clip line
            if (!polygon.getLines().isEmpty()) {
                Line subject = polygon.getLines().getLast();
                Line clipped;
                try {
                    clipped = clipperLine.clip(subject, clippingTriangle);
                } catch (UnsupportedOperationException unfinishedClipper) {
                    clipped = null;
                }

                if (clipped != null) {
                    Polygon rasterizePolygon = new Polygon();
                    rasterizePolygon.addLine(clipped);

                    polygonRasterizer.rasterize(withColor(rasterizePolygon, ORIGINAL_COLOR), null);
                    //new XiaolinWuLineRasterizer(canvas.getRaster()).rasterize(clipped);
                }
            } else {
                polygonRasterizer.rasterize(withColor(polygon, ORIGINAL_COLOR), previewLine);
            }
        }

        polygonRasterizer.rasterize(clippingTriangle, null);

        if (seedFillPoint != null) {
            seedFiller.fill(seedFillPoint.getX(), seedFillPoint.getY(), canvas.getBackgroundColor(), FILL_COLOR);
        }
        canvas.repaint();
    }

    private void clearDrawing() {
        polygon.clear();
        previewLine = null;
        seedFillPoint = null;
        seedFillMode = 1;
        toolbar.setSeedFillMode(seedFillMode);
        render();
    }


    private void cycleClippingMode() {
        clippingMode += 1;
        if (clippingMode == 3) clippingMode = 1;
        if (clippingMode == 1) { // Polygon
            this.clippingTriangle = createPolygon(HODGMAN_CLIPPING_TRIANGLE_VERTICES, CLIPPING_TRIANGLE_COLOR);
        } else if (clippingMode == 2) { // Úsečka
            this.clippingTriangle = createPolygon(COHEN_CLIPPING_POLYGON_VERTICES, CLIPPING_TRIANGLE_COLOR);
        }

        toolbar.setClippingMode(clippingMode);
        clearDrawing();
        render();
    }

    private void cycleSeedFillMode() {
        seedFillMode += 1;
        if (seedFillMode == 4) seedFillMode = 1;
        if (seedFillMode == 1) {
            seedFillPoint = null;
        }
        toolbar.setSeedFillMode(seedFillMode);
        render();
    }

    private void fillAt(MouseEvent event) {
        Point seed = getPoint(event);
        Integer seedColor = canvas.getRaster().getPixel(seed.getX(), seed.getY());
        if (seedFillPoint != null && seedColor != null && seedColor == FILL_COLOR) {
            seedFillPoint = null;
            render();
            return;
        }

        boolean filled = seedFiller.fill(seed.getX(), seed.getY(), canvas.getBackgroundColor(), FILL_COLOR);
        if (filled) {
            seedFillPoint = seed;
            render();
        }
    }

    private Point getPoint(MouseEvent event) {
        return new Point(event.getX(), event.getY());
    }

    private void startLine(MouseEvent event) {
        Point startPoint = polygon.getLastPoint();
        if (startPoint == null) {
            startPoint = getPoint(event);
        }
        previewLine = new Line(startPoint, startPoint, PREVIEW_COLOR);
        render();
    }

    private Point getEndLinePoint(MouseEvent event, Point startPoint) {
        Point targetPoint = getPoint(event);
        if (!event.isShiftDown()) return targetPoint;

        // Teď vypočítáme úhlopříčku/horizontální/svislou čáru
        int dx = targetPoint.getX() - startPoint.getX();
        int dy = targetPoint.getY() - startPoint.getY();

        double angle = Math.atan2(dy, dx);
        double length = Math.hypot(dx, dy);

        double snappedAngle = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
        int finalX = startPoint.getX() + (int) Math.round(length * Math.cos(snappedAngle));
        int finalY = startPoint.getY() + (int) Math.round(length * Math.sin(snappedAngle));
        return new Point(finalX, finalY);
    }

    private Polygon withColor(Polygon source, int color) {
        Polygon result = new Polygon();
        for (Line line : source.getLines()) {
            result.addLine(new Line(line.getPoint1(), line.getPoint2(), color));
        }
        return result;
    }

    private Polygon withPreviewLine(Polygon source, Line preview) {
        Polygon result = new Polygon();
        for (Line line : source.getLines()) {
            result.addLine(line);
        }
        if (preview != null) {
            result.addLine(preview);
        }
        return result;
    }

    private Polygon createPolygon(List<? extends Point> vertices, int color) {
        Polygon result = new Polygon();
        for (int i = 0; i + 1 < vertices.size(); i++) {
            result.addLine(new Line(vertices.get(i), vertices.get(i + 1), color));
        }
        return result;
    }

}
