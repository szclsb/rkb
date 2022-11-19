package ch.szclsb.rkb.app;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.impl.Receiver;
import ch.szclsb.rkb.comm.impl.Sender;
import javafx.beans.property.Property;
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

    private final Property<Mode> modeProperty;
    private final Sender sender;
    private final Receiver receiver;
    private final Consumer<Integer> vkCodeHandler;

    public FxController() {
        this.vkCodeHandler = System.out::println;
        this.modeProperty = new SimpleObjectProperty<>();
        Consumer<ChannelState> stateHandler = state1 -> {
            state.setText(state1.name());
            area.setDisable(!(this.modeProperty.getValue().equals(Mode.SEND) && state1.equals(ChannelState.CONNECTED)));
        };
        Consumer<Throwable> errorHandler = t -> System.err.println(t.getMessage());
        this.sender = new Sender(errorHandler, stateHandler);
        this.receiver = new Receiver(errorHandler, stateHandler);
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

        state.setText(ChannelState.DISCONNECTED.name());
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
            }
            case RECEIVE -> {
                var host = remoteAddressInput.getText();
                var port = Integer.parseInt(remotePortInput.getText());
                receiver.connect(host, port, vkCodeHandler);
            }
            default -> {
                System.out.println("error");
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
