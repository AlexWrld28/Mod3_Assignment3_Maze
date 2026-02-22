import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class MazeFXApp extends Application {

    @Override
    public void start(Stage stage) {
        TabPane tabPane = new TabPane();

        Tab maze1Tab = new Tab("Maze 1", new MazePane("assets/maze.png", "assets/robot.png"));
        Tab maze2Tab = new Tab("Maze 2", new MazePane("assets/maze2.png", "assets/robot.png"));

        maze1Tab.setClosable(false);
        maze2Tab.setClosable(false);

        tabPane.getTabs().addAll(maze1Tab, maze2Tab);

        Scene scene = new Scene(tabPane);
        stage.setTitle("Maze Droid / Car Project (JavaFX)");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}