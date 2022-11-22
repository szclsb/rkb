module rkb.app.main {
    requires javafx.controls;
    requires javafx.fxml;

    requires ch.szclsb.rkb.driver;
    requires ch.szclsb.rkb.comm;

    opens ch.szclsb.rkb.app to javafx.fxml, javafx.graphics;
    exports ch.szclsb.rkb.app to javafx.fxml;
}
