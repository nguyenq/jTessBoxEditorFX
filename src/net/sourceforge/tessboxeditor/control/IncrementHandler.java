package net.sourceforge.tessboxeditor.control;

import javafx.animation.AnimationTimer;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * @author fabian
 * https://stackoverflow.com/questions/41050085/javafx-spinner-change-is-slow-with-click-and-hold-of-mouse-button
 */
public class IncrementHandler implements EventHandler<MouseEvent> {

    private Spinner spinner;
    private boolean increment;
    private long startTimestamp;
    private static final PseudoClass PRESSED = PseudoClass.getPseudoClass("pressed");

    private static final long DELAY = 1000l * 1000L * 750L; // 0.75 sec
    private Node button;

    private final AnimationTimer timer = new AnimationTimer() {

        @Override
        public void handle(long now) {
            if (now - startTimestamp >= DELAY) {
                // trigger updates every frame once the initial delay is over
                if (increment) {
                    spinner.increment();
                } else {
                    spinner.decrement();
                }
            }
        }
    };

    @Override
    public void handle(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            Spinner source = (Spinner) event.getSource();
            Node node = event.getPickResult().getIntersectedNode();

            Boolean increment = null;
            // find which kind of button was pressed and if one was pressed
            while (increment == null && node != source) {
                if (node.getStyleClass().contains("increment-arrow-button")) {
                    increment = Boolean.TRUE;
                } else if (node.getStyleClass().contains("decrement-arrow-button")) {
                    increment = Boolean.FALSE;
                } else {
                    node = node.getParent();
                }
            }
            if (increment != null) {
                event.consume();
                source.requestFocus();
                spinner = source;
                this.increment = increment;

                // timestamp to calculate the delay
                startTimestamp = System.nanoTime();

                button = node;

                // update for css styling
                node.pseudoClassStateChanged(PRESSED, true);

                // first value update
                timer.handle(startTimestamp + DELAY);

                // trigger timer for more updates later
                timer.start();
            }
        }
    }

    public void stop() {
        timer.stop();
        button.pseudoClassStateChanged(PRESSED, false);
        button = null;
        spinner = null;
    }
}
