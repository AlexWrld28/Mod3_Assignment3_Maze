package src;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.File;
import java.util.*;

public class MazePane extends BorderPane {

    private static final int WALKABLE_BRIGHTNESS_THRESHOLD = 150; // adjust if needed

    private final Image mazeImage;
    private final Image robotImage;
    private final PixelReader pixelReader;

    private final Canvas canvas;
    private final GraphicsContext gc;

    private final Label statusLabel = new Label("Ready");
    private final Label sensorLabel = new Label("Sensor:");
    private final Button autoBtn = new Button("Start Auto Solve");
    private final Button resetBtn = new Button("Reset");
    private final CheckBox carModeCheck = new CheckBox("Use Drawn Car");

    private final CarSprite carSprite = new CarSprite();

    private int x, y;                    // sprite center
    private int spriteSize = 24;
    private Direction heading = Direction.RIGHT;

    private GridPoint startPoint;
    private GridPoint exitPoint;

    private final List<GridPoint> currentPath = new ArrayList<>();
    private int pathIndex = 0;
    private Timeline animation;

    public MazePane(String mazePath, String robotPath) {
        mazeImage = new Image(new File(mazePath).toURI().toString());
        robotImage = new Image(new File(robotPath).toURI().toString());

        if (mazeImage.isError()) {
            throw new RuntimeException("Failed to load maze image: " + mazePath);
        }
        if (robotImage.isError()) {
            throw new RuntimeException("Failed to load robot image: " + robotPath);
        }

        pixelReader = mazeImage.getPixelReader();

        canvas = new Canvas(mazeImage.getWidth(), mazeImage.getHeight());
        gc = canvas.getGraphicsContext2D();

        // Top controls
        HBox top = new HBox(10, autoBtn, resetBtn, carModeCheck, statusLabel);
        top.setPadding(new Insets(8));

        // Bottom status
        HBox bottom = new HBox(10, sensorLabel);
        bottom.setPadding(new Insets(8));

        setTop(top);
        setCenter(new StackPane(canvas));
        setBottom(bottom);

        detectStartAndExit();
        resetToStart();

        // Key handling (JavaFX equivalent of KeyListener)
        canvas.setFocusTraversable(true);
        canvas.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        canvas.setOnMouseClicked(e -> canvas.requestFocus());

        autoBtn.setOnAction(e -> startAutoSolve());
        resetBtn.setOnAction(e -> {
            stopAnimation();
            resetToStart();
            canvas.requestFocus();
        });

        carModeCheck.setOnAction(e -> {
            draw();
            canvas.requestFocus();
        });

        draw();
    }

    // =============================
    // Drawing
    // =============================
    private void draw() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.drawImage(mazeImage, 0, 0);

        // Markers
        if (startPoint != null) {
            gc.setFill(Color.rgb(0, 200, 0, 0.7));
            gc.fillOval(startPoint.x - 6, startPoint.y - 6, 12, 12);
        }
        if (exitPoint != null) {
            gc.setFill(Color.rgb(255, 140, 0, 0.7));
            gc.fillOval(exitPoint.x - 6, exitPoint.y - 6, 12, 12);
        }

        // Sprite: robot image or drawn car
        if (carModeCheck.isSelected()) {
            carSprite.draw(gc, x, y, 26, heading);
        } else {
            gc.drawImage(robotImage, x - spriteSize / 2.0, y - spriteSize / 2.0, spriteSize, spriteSize);
        }
    }

    // =============================
    // Input / movement
    // =============================
    private void handleKeyPress(KeyEvent e) {
        if (animation != null && animation.getStatus().name().equals("RUNNING")) {
            return;
        }

        int step = 4;
        KeyCode code = e.getCode();

        if (code == KeyCode.LEFT) {
            heading = Direction.LEFT;
            tryMove(-step, 0);
        } else if (code == KeyCode.RIGHT) {
            heading = Direction.RIGHT;
            tryMove(step, 0);
        } else if (code == KeyCode.UP) {
            heading = Direction.UP;
            tryMove(0, -step);
        } else if (code == KeyCode.DOWN) {
            heading = Direction.DOWN;
            tryMove(0, step);
        }
    }

    private void tryMove(int dx, int dy) {
        int nx = x + dx;
        int ny = y + dy;

        if (canOccupy(nx, ny)) {
            x = nx;
            y = ny;
            statusLabel.setText("Moved " + heading);
        } else {
            statusLabel.setText("Blocked by wall");
        }

        updateSensorLabel();
        draw();

        if (exitPoint != null && distance(x, y, exitPoint.x, exitPoint.y) < 8) {
            statusLabel.setText("Reached exit manually!");
        }
    }

    // =============================
    // Auto-solve (BFS + animation)
    // =============================
    private void startAutoSolve() {
        stopAnimation();

        statusLabel.setText("Computing path...");
        List<GridPoint> path = bfsPath(new GridPoint(x, y), exitPoint, 2);

        if (path == null || path.isEmpty()) {
            statusLabel.setText("No path found (try adjusting threshold)");
            return;
        }

        currentPath.clear();
        currentPath.addAll(path);
        pathIndex = 0;

        autoBtn.setDisable(true);
        statusLabel.setText("Auto-solving...");

        animation = new Timeline(new KeyFrame(Duration.millis(12), e -> stepAnimation()));
        animation.setCycleCount(Timeline.INDEFINITE);
        animation.play();
        canvas.requestFocus();
    }

    private void stepAnimation() {
        if (pathIndex >= currentPath.size()) {
            stopAnimation();
            statusLabel.setText("Done!");
            return;
        }

        GridPoint next = currentPath.get(pathIndex++);
        int dx = next.x - x;
        int dy = next.y - y;

        if (Math.abs(dx) > Math.abs(dy)) {
            heading = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else if (dy != 0) {
            heading = dy > 0 ? Direction.DOWN : Direction.UP;
        }

        x = next.x;
        y = next.y;

        updateSensorLabel();
        draw();

        if (distance(x, y, exitPoint.x, exitPoint.y) < 5) {
            stopAnimation();
            statusLabel.setText("Reached exit!");
        }
    }

    private void stopAnimation() {
        if (animation != null) {
            animation.stop();
        }
        autoBtn.setDisable(false);
    }

    // =============================
    // Maze / sensing / collision
    // =============================
    private void detectStartAndExit() {
        List<GridPoint> openings = findBorderOpenings();

        if (openings.size() >= 2) {
            startPoint = openings.get(0);

            GridPoint farthest = openings.get(1);
            double bestDist = distance(startPoint.x, startPoint.y, farthest.x, farthest.y);

            for (GridPoint p : openings) {
                double d = distance(startPoint.x, startPoint.y, p.x, p.y);
                if (d > bestDist) {
                    bestDist = d;
                    farthest = p;
                }
            }
            exitPoint = farthest;
        } else {
            startPoint = findFirstWalkable();
            exitPoint = findLastWalkable();
        }

        startPoint = nudgeToFit(startPoint);
        exitPoint = nudgeToFit(exitPoint);

        if (startPoint == null || exitPoint == null) {
            throw new RuntimeException("Could not detect valid start/exit on maze.");
        }
    }

    private void resetToStart() {
        x = startPoint.x;
        y = startPoint.y;
        heading = Direction.RIGHT;
        statusLabel.setText("Manual mode (click maze, then use arrow keys)");
        updateSensorLabel();
        draw();
    }

    private void updateSensorLabel() {
        if (!inBounds(x, y)) {
            sensorLabel.setText("Sensor: out of bounds");
            return;
        }

        Color c = pixelReader.getColor(x, y);
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);

        sensorLabel.setText("Sensor @ (" + x + "," + y + ") -> RGB(" + r + "," + g + "," + b + ") | Heading: " + heading);
    }

    private boolean canOccupy(int cx, int cy) {
        int r = spriteSize / 2 - 2;

        int[][] offsets = {
                {0, 0},
                {r, 0}, {-r, 0}, {0, r}, {0, -r},
                {r, r}, {r, -r}, {-r, r}, {-r, -r}
        };

        for (int[] o : offsets) {
            int px = cx + o[0];
            int py = cy + o[1];

            if (!inBounds(px, py)) return false;
            if (!isWalkable(px, py)) return false;
        }
        return true;
    }

    private boolean isWalkable(int px, int py) {
        Color c = pixelReader.getColor(px, py);
        double brightness255 = ((c.getRed() + c.getGreen() + c.getBlue()) / 3.0) * 255.0;
        return brightness255 >= WALKABLE_BRIGHTNESS_THRESHOLD;
    }

    private boolean inBounds(int px, int py) {
        return px >= 0 && py >= 0 && px < (int) mazeImage.getWidth() && py < (int) mazeImage.getHeight();
    }

    private GridPoint findFirstWalkable() {
        for (int yy = 0; yy < (int) mazeImage.getHeight(); yy++) {
            for (int xx = 0; xx < (int) mazeImage.getWidth(); xx++) {
                if (isWalkable(xx, yy)) return new GridPoint(xx, yy);
            }
        }
        return null;
    }

    private GridPoint findLastWalkable() {
        for (int yy = (int) mazeImage.getHeight() - 1; yy >= 0; yy--) {
            for (int xx = (int) mazeImage.getWidth() - 1; xx >= 0; xx--) {
                if (isWalkable(xx, yy)) return new GridPoint(xx, yy);
            }
        }
        return null;
    }

    private List<GridPoint> findBorderOpenings() {
        List<GridPoint> raw = new ArrayList<>();
        int w = (int) mazeImage.getWidth();
        int h = (int) mazeImage.getHeight();

        for (int x = 0; x < w; x++) {
            if (isWalkable(x, 0)) raw.add(new GridPoint(x, 0));
            if (isWalkable(x, h - 1)) raw.add(new GridPoint(x, h - 1));
        }
        for (int y = 0; y < h; y++) {
            if (isWalkable(0, y)) raw.add(new GridPoint(0, y));
            if (isWalkable(w - 1, y)) raw.add(new GridPoint(w - 1, y));
        }

        // Merge nearby points so a doorway isn't counted 50 times
        List<GridPoint> merged = new ArrayList<>();
        int minGap = 20;
        for (GridPoint p : raw) {
            boolean close = false;
            for (GridPoint m : merged) {
                if (distance(p.x, p.y, m.x, m.y) < minGap) {
                    close = true;
                    break;
                }
            }
            if (!close) merged.add(p);
        }
        return merged;
    }

    private GridPoint nudgeToFit(GridPoint p) {
        if (p == null) return null;
        if (canOccupy(p.x, p.y)) return p;

        for (int radius = 1; radius <= 40; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int nx = p.x + dx;
                    int ny = p.y + dy;
                    if (inBounds(nx, ny) && canOccupy(nx, ny)) {
                        return new GridPoint(nx, ny);
                    }
                }
            }
        }
        return null;
    }

    // =============================
    // BFS pathfinding
    // =============================
    private List<GridPoint> bfsPath(GridPoint start, GridPoint goal, int step) {
        if (start == null || goal == null) return null;

        int w = (int) mazeImage.getWidth();
        int h = (int) mazeImage.getHeight();

        int sx = clamp((start.x / step) * step, 0, w - 1);
        int sy = clamp((start.y / step) * step, 0, h - 1);
        int gx = clamp((goal.x / step) * step, 0, w - 1);
        int gy = clamp((goal.y / step) * step, 0, h - 1);

        if (!canOccupy(sx, sy)) {
            GridPoint s2 = nudgeToFit(new GridPoint(sx, sy));
            if (s2 == null) return null;
            sx = (s2.x / step) * step;
            sy = (s2.y / step) * step;
        }

        if (!canOccupy(gx, gy)) {
            GridPoint g2 = nudgeToFit(new GridPoint(gx, gy));
            if (g2 == null) return null;
            gx = (g2.x / step) * step;
            gy = (g2.y / step) * step;
        }

        int gridW = (w + step - 1) / step;
        int gridH = (h + step - 1) / step;

        int sIndex = (sy / step) * gridW + (sx / step);
        int gIndex = (gy / step) * gridW + (gx / step);

        boolean[] visited = new boolean[gridW * gridH];
        int[] parent = new int[gridW * gridH];
        Arrays.fill(parent, -1);

        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(sIndex);
        visited[sIndex] = true;

        int[] dirs = {1, 0, -1, 0, 1}; // 4-neighbors

        while (!q.isEmpty()) {
            int cur = q.removeFirst();
            if (cur == gIndex) break;

            int cx = cur % gridW;
            int cy = cur / gridW;

            for (int i = 0; i < 4; i++) {
                int nx = cx + dirs[i];
                int ny = cy + dirs[i + 1];

                if (nx < 0 || ny < 0 || nx >= gridW || ny >= gridH) continue;

                int ni = ny * gridW + nx;
                if (visited[ni]) continue;

                int px = nx * step;
                int py = ny * step;

                if (!inBounds(px, py)) continue;
                if (!canOccupy(px, py)) continue;

                visited[ni] = true;
                parent[ni] = cur;
                q.addLast(ni);
            }
        }

        if (!visited[gIndex]) return null;

        LinkedList<GridPoint> path = new LinkedList<>();
        int cur = gIndex;
        while (cur != sIndex) {
            int cx = cur % gridW;
            int cy = cur / gridW;
            path.addFirst(new GridPoint(cx * step, cy * step));
            cur = parent[cur];
        }

        return path;
    }

    // =============================
    // Helpers
    // =============================
    private static double distance(int x1, int y1, int x2, int y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static class GridPoint {
        final int x;
        final int y;

        GridPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}