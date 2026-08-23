import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Minimal repro for: macOS swallows the first click on the red title-bar close button
 * of a showAndWait() window after any interaction with the window's content.
 *
 * Test per variant: open the modal, click the checkbox inside it once,
 * then click the modal's red X. Count clicks until it closes.
 *
 * Variant 3 (show + Platform.enterNestedEventLoop) is showAndWait() minus everything
 * else it does - it isolates whether the nested event loop alone causes the bug.
 */
public class CloseClickRepro extends Application {

    private static final int WAIT = 0, SHOW = 1, LOOP = 2;

    @Override
    public void start(Stage primary) {
        Button waitBtn = new Button("1: showAndWait()");
        Button showBtn = new Button("2: show()");
        Button loopBtn = new Button("3: show() + enterNestedEventLoop");
        waitBtn.setOnAction(e -> openModal(primary, WAIT));
        showBtn.setOnAction(e -> openModal(primary, SHOW));
        loopBtn.setOnAction(e -> openModal(primary, LOOP));

        VBox root = new VBox(10, new Label("Minimal close-click repro"), waitBtn, showBtn, loopBtn);
        root.setPadding(new Insets(20));
        primary.setScene(new Scene(root, 340, 180));
        primary.setTitle("Repro launcher");
        primary.show();
    }

    private void openModal(Stage owner, int mode) {
        String name = switch (mode) {
            case WAIT -> "showAndWait";
            case SHOW -> "show";
            default -> "show+nestedEventLoop";
        };
        Stage stage = new Stage();
        CheckBox box = new CheckBox("Click me first, then click the red X");
        VBox root = new VBox(10, new Label(name), box);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 300, 120));
        stage.setTitle(name);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setOnCloseRequest(e -> System.out.println("CLOSE_REQUEST reached JavaFX (" + name + ")"));

        switch (mode) {
            case WAIT -> stage.showAndWait();
            case SHOW -> stage.show();
            case LOOP -> {
                Object key = new Object();
                stage.setOnHidden(e -> Platform.exitNestedEventLoop(key, null));
                stage.show();
                Platform.enterNestedEventLoop(key);
                System.out.println("nested event loop exited (" + name + ")");
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
