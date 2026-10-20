module pl.projekt.szachy.web {
    requires pl.projekt.szachy.core;
    requires pl.projekt.szachy.computer;
    requires jdk.httpserver;
    requires com.google.gson;

    exports pl.projekt.szachy.web;
}
