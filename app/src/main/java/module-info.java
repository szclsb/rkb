module rkb.app.main {
    requires javafx.controls;
    requires javafx.fxml;

    requires rkb.driver.main;
    requires rkb.connection.main;

    opens ch.szclsb.rkb.app to javafx.fxml, javafx.graphics;
    exports ch.szclsb.rkb.app to javafx.fxml;
}
