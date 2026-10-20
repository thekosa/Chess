module pl.projekt.szachy.desktop {
    requires pl.projekt.szachy.core;
    requires pl.projekt.szachy.console;
    requires pl.projekt.szachy.web;
    requires javafx.controls;
    requires javafx.fxml;
    // Optional ImageIO support for board preview generation.
    requires static java.desktop;

    exports pl.projekt.szachy.desktop to javafx.graphics;
    opens pl.projekt.szachy.desktop to javafx.fxml;
}
