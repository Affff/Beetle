package ru.obolensk.afff.beetle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Afff on 10.04.2017.
 */
public class ConsoleClient {

    private static BeetleServer server;

    public static void main(@Nonnull String[] args) throws IOException {
        server = new BeetleServer(4080);
        printMenu();
    }

    private static void printMenu() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))){
            printf("Welcome to %s!", Version.nameAndVersion());
            while (true) {
                printf("Menu:");
                printf("q) Shutdown server");
                printf("Enter the command:");
                final String cmd = reader.readLine();
                if ("q".equals(cmd)) {
                    server.close();
                    break;
                }
            }
        }
    }

    private static void printf(@Nonnull final String msg, @Nullable final Object... params) {
        if (params != null && params.length != 0) {
            System.out.printf(msg + "\r\n", params);
        } else {
            System.out.println(msg);
        }
    }
}
