package ch.szclsb.rkb.app;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.impl.ReceiverChannel;
import ch.szclsb.rkb.comm.impl.SenderChannel;
import ch.szclsb.rkb.driver.impl.KeyboardDriver;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

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
    private CommState stateComponent;
    @FXML
    private TextArea area;

    private final Property<Mode> modeProperty;
    private final SenderChannel sender;
    private final ReceiverChannel receiver;
    private final KeyboardDriver keyboard;

    public FxController() {
        this.keyboard = KeyboardDriver.getInstance();
        this.modeProperty = new SimpleObjectProperty<>();
        this.sender = new SenderChannel();
        this.sender.addStateChangeListener(state -> {
            stateComponent.stateObserverProperty().set(state);
            area.setDisable(state != ChannelState.CONNECTED);
        });
        this.receiver = new ReceiverChannel();
        this.receiver.addStateChangeListener(state -> {
            stateComponent.stateObserverProperty().set(state);
        });
        this.modeProperty.addListener((observable, oldValue, newValue) -> {
            action.setText(newValue.getActionText());
            remoteAddressInput.setDisable(!newValue.equals(Mode.RECEIVE));
        });
    }

    public void initialize() {
        remoteAddressLabel.setText("remote address");
        remotePortLabel.setText("remote port");
        sendMode.setText("send");
        receiveMode.setText("receive");
        area.setDisable(true);
        area.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (ChannelState.CONNECTED.equals(sender.getState())) {
                if (newValue) {
                    keyboard.scan();
                } else {
                    keyboard.stop();
                }
            }
        });

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
        try {
            switch (modeProperty.getValue()) {
                case SEND -> {
                    var port = Integer.parseInt(remotePortInput.getText());
                    sender.open(port);
                    keyboard.scan();
                }
                case RECEIVE -> {
                    var host = remoteAddressInput.getText();
                    var port = Integer.parseInt(remotePortInput.getText());
                    receiver.connect(host, port, keyboard::invoke);
                }
                default -> System.err.println("error");
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    /**
     * Release acquired resources.
     *
     * @throws Exception
     */
    public void terminate() throws Exception {
        sender.terminate();
        receiver.disconnect();
        keyboard.close();
    }
}
