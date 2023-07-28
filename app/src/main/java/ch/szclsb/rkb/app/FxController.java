package ch.szclsb.rkb.app;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.impl.SenderChannel;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;

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
//    private final IChannel receiver;

    public FxController() {
        this.modeProperty = new SimpleObjectProperty<>();
//        Consumer<Throwable> errorHandler = t -> System.err.println(t.getMessage());
        this.sender = new SenderChannel();
        this.sender.addStateChangeListener(state -> {
            stateComponent.stateObserverProperty().set(state);
            area.setDisable(state != ChannelState.CONNECTED);
        });
//        this.receiver = new Receiver(errorHandler);
//        this.receiver.addStateChangeListener(state -> {
//            stateComponent.stateObserverProperty().set(state);
//        });
//        this.receiver.addVkCodeListener(System.out::println);
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
                }
                case RECEIVE -> {
                    var host = remoteAddressInput.getText();
                    var port = Integer.parseInt(remotePortInput.getText());
//                receiver.connect(host, port);
                }
                default -> {
                    System.err.println("error");
                }
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    // todo use native driver scanner

    @FXML
    private void onKeyDown(KeyEvent event) {
        sender.send(event.getCode().getCode(), false);
    }
    @FXML
    private void onKeyUp(KeyEvent event) {
        sender.send(event.getCode().getCode(), true);
    }

    /**
     * Release acquired resources.
     *
     * @throws Exception
     */
    public void terminate() throws Exception {
        sender.close();
//        receiver.close();
    }
}
