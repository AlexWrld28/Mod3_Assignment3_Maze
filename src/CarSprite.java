package src;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CarSprite  {

    public void draw(GraphicsContext gc, double cx, double cy, double size, Direction heading)  {
        gc.save();

        gc.translate(cx, cy);

        double angle = switch (heading) {
            case RIGHT -> 0;
            case DOWN -> 90;
            case LEFT -> 180;
            case UP -> -90;
        };
        gc.rotate(angle);

        double w = size;
        double h = size * 0.55;
        // Body (rect)
        gc.setFill(Color.rgb(200, 30, 30));
        gc.fillRoundRect(-w / 2, -h / 2, w, h, 10, 10);

        // Roof (rect)
        gc.setFill(Color.rgb(225, 70, 70));
        gc.fillRoundRect(-w * 0.15, -h * 0.8, w * 0.55, h * 0.45, 8, 8);

        // Front marker (polygon) to show heading
        double[] px = {w / 2, w / 2 - 10, w / 2 - 10};
        double[] py = {0, -8, 8};
        gc.setFill(Color.GOLD);
        gc.fillPolygon(px, py, 3);

        // Windows (rect)
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRoundRect(-w * 0.05, -h * 0.72, w * 0.28, h * 0.25, 6, 6);

        // Wheels (ovals)
        gc.setFill(Color.BLACK);
        gc.fillOval(-w * 0.32, h * 0.28, 10, 10);
        gc.fillOval(w * 0.08, h * 0.28, 10, 10);
        gc.fillOval(-w * 0.32, -h * 0.42, 10, 10);
        gc.fillOval(w * 0.08, -h * 0.42, 10, 10);

        // Rims
        gc.setFill(Color.LIGHTGRAY);
        gc.fillOval(-w * 0.32 + 2.5, h * 0.28 + 2.5, 5, 5);
        gc.fillOval(w * 0.08 + 2.5, h * 0.28 + 2.5, 5, 5);
        gc.fillOval(-w * 0.32 + 2.5, -h * 0.42 + 2.5, 5, 5);
        gc.fillOval(w * 0.08 + 2.5, -h * 0.42 + 2.5, 5, 5);

        gc.restore();
    }
}