package ch.szclsb.rkb.app;

import ch.szclsb.rkb.comm.ChannelState;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import static javafx.scene.paint.Color.*;

public class CommState extends Pane {
    private final ObjectProperty<ChannelState> stateObserver;

    public CommState() {
        var circle = new Circle( 5);
        stateObserver = new SimpleObjectProperty<>();
        stateObserver.addListener((observable, oldValue, newValue) -> {
            circle.setFill(switch (newValue) {
                case DISCONNECTED -> RED;
                case WAITING -> YELLOW;
                case CONNECTED -> GREEN;
                case STARTING -> ORANGE;
                case CONNECTING -> LIGHTGREEN;
                case DISCONNECTING -> BLUE;
                case STOPPING -> LIGHTBLUE;
                case TERMINATING -> BLACK;
            });
        });
        stateObserver.set(ChannelState.DISCONNECTED);
        circle.centerXProperty().bind(widthProperty().divide(2));
        circle.centerYProperty().bind(heightProperty().divide(2));
        circle.radiusProperty().bind(Bindings.min(widthProperty(), heightProperty()).divide(2));
        getChildren().add(circle);
    }

    public ChannelState getStateObserver() {
        return stateObserver.getValue();
    }

    public ObjectProperty<ChannelState> stateObserverProperty() {
        return stateObserver;
    }
}
