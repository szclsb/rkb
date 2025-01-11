package ch.szclsb.rkb.app;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.impl.ReceiverChannel;
import ch.szclsb.rkb.comm.impl.SenderChannel;
import ch.szclsb.rkb.driver.impl.KeyboardDriver;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FxController {
    private static final Logger log = LoggerFactory.getLogger(FxController.class);

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
            Platform.runLater(() -> {
                stateComponent.stateObserverProperty().set(state);
                area.setDisable(state != ChannelState.CONNECTED);
                action.setDisable(state != ChannelState.DISCONNECTED
                        && state != ChannelState.WAITING
                        && state != ChannelState.CONNECTED);
                modeProperty.setValue(switch (state) {
                    case CONNECTED -> Mode.SENDING;
                    case WAITING -> Mode.WAITING;
                    default -> Mode.SEND;
                });
            });

        });
        this.receiver = new ReceiverChannel();
        this.receiver.addStateChangeListener(state -> {
            Platform.runLater(() -> {
                stateComponent.stateObserverProperty().set(state);
                action.setDisable(state != ChannelState.DISCONNECTED
                        && state != ChannelState.WAITING
                        && state != ChannelState.CONNECTED);
                modeProperty.setValue(switch (state) {
                    case CONNECTED -> Mode.SENDING;
                    case WAITING -> Mode.WAITING;
                    default -> Mode.SEND;
                });
            });
        });
        this.modeProperty.addListener((observable, oldValue, newValue) -> {
            log.info("Mode: {}", newValue);
            action.setText(newValue.getActionText());
//            remoteAddressInput.setDisable(!newValue.equals(Mode.RECEIVE));
            remoteAddressInput.setDisable(newValue.isSend());
        });
    }

    private void startKeyboardScanner() {
        Thread.ofVirtual().start(keyboard::scan);
    }

    private void stopKeyboardScanner() {
        Thread.ofVirtual().start(keyboard::stop);
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
                    startKeyboardScanner();
                    log.info("Started keyboard scanner");
                } else {
                    stopKeyboardScanner();
                    log.info("Stopped keyboard scanner");
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
                    log.info("Started sender on port {}", port);
                    startKeyboardScanner();
                    log.info("Started keyboard scanner");
                }
                case WAITING -> {
                    sender.terminate();
                    log.info("Stopped sender");
                }
                case SENDING -> {
                    sender.disconnect();
                    log.info("Disconnected sender");
                }
                case RECEIVE -> {
                    var host = remoteAddressInput.getText();
                    var port = Integer.parseInt(remotePortInput.getText());
                    receiver.connect(host, port, keyboard::invoke);
                    log.info("Started receiver listening on {}:{}", host, port);
                }
                case RECEIVING -> {
                    receiver.disconnect();
                    log.info("Stopped receiver");
                }
            }
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
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
