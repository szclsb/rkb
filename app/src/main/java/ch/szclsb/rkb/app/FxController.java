package ch.szclsb.rkb.app;

import ch.szclsb.rkb.connection.Receiver;
import ch.szclsb.rkb.connection.Sender;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;

import java.util.function.Consumer;

public class FxController {
    @FXML
    private Label remoteAddressLabel;
    @FXML
    private Label remotePortLabel;
    @FXML
    private TextField remoteAddressInput;
    @FXML
    private TextField remotePortInput;
    @FXML
    private RadioButton sendMode;
    @FXML
    private RadioButton receiveMode;
    @FXML
    private Button action;
    @FXML
    private Label state;
    @FXML
    private TextArea area;

    private final BooleanProperty connectedProperty;
    private final Property<Mode> modeProperty;
    private final Sender sender;
    private final Receiver receiver;

    public FxController() {
        Consumer<Throwable> errorHandler = t -> System.err.println(t.getMessage());
        this.sender = new Sender(errorHandler);
        this.receiver = new Receiver(System.out::println, errorHandler);

        this.modeProperty = new SimpleObjectProperty<>();
        this.modeProperty.addListener((observable, oldValue, newValue) -> {
            action.setText(newValue.getActionText());
            remoteAddressInput.setDisable(!newValue.equals(Mode.RECEIVE));
        });
        this.connectedProperty = new SimpleBooleanProperty(false);
        connectedProperty.addListener((observable, oldValue, newValue) -> {
            state.setText(newValue ? "connected" : "disconnected");
            state.setDisable(!newValue || !Mode.SEND.equals(modeProperty.getValue()));
            area.setDisable(!newValue || !Mode.SEND.equals(modeProperty.getValue()));
        });
    }

    public void initialize() {
        remoteAddressLabel.setText("remote address");
        remotePortLabel.setText("remote port");
        sendMode.setText("send");
        receiveMode.setText("receive");

        state.setText("disconnected");
        area.setDisable(true);

        sendMode.fire();
    }

    @FXML
    private void onSendMode(ActionEvent event) {
        modeProperty.setValue(Mode.SEND);
    }

    @FXML
    private void onReceiveMode(ActionEvent event) {
        modeProperty.setValue(Mode.RECEIVE);
    }

    @FXML
    private void onAction(ActionEvent event) {
        switch (modeProperty.getValue()) {
            case SEND -> {
                var port = Integer.parseInt(remotePortInput.getText());
                sender.open(port);
                connectedProperty.setValue(true);
            }
            case RECEIVE -> {
                var host = remoteAddressInput.getText();
                var port = Integer.parseInt(remotePortInput.getText());
                receiver.connect(host, port);
                connectedProperty.setValue(true);
            }
            default -> {
                System.out.println("error");
                connectedProperty.setValue(false);
            }
        }
    }

    @FXML
    private void onKeyDown(KeyEvent event) {
        sender.send(event.getCode().getCode());
    }

    /**
     * Release acquired resources.
     *
     * @throws Exception
     */
    public void terminate() throws Exception {
        sender.close();
        receiver.close();
    }
}
