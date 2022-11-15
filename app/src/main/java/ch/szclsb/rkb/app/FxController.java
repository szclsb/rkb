package ch.szclsb.rkb.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

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

    public void initialize() {
        remoteAddressLabel.setText("remote address");
        remotePortLabel.setText("remote port");
        sendMode.setText("send");
        receiveMode.setText("receive");
    }
}
