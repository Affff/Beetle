package ru.obolensk.afff.beetle.util;

/**
 * Created by Afff on 11.04.2017.
 */
public class Version {

    private static final String SERVER_NAME = "Beetle HTTP server";
    private static final String SERVER_VERSION = "0.0.1";
    private static final String SERVER_NAME_AND_VERSION = SERVER_NAME + " v" + SERVER_VERSION;

    public static String nameAndVersion() {
        return SERVER_NAME_AND_VERSION;
    }
}
