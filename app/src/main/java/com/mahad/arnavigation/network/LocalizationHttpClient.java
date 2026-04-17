package com.mahad.arnavigation.network;

import com.google.gson.Gson;
import com.mahad.arnavigation.data.PoseMock;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LocalizationHttpClient {
    private final Gson gson;

    public LocalizationHttpClient() {
        this.gson = new Gson();
    }

    public PoseMock fetchPose(String endpointUrl) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpointUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2500);
            connection.setReadTimeout(2500);
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("Localization HTTP " + code);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new BufferedInputStream(connection.getInputStream()),
                            StandardCharsets.UTF_8
                    )
            )) {
                PoseMock pose = gson.fromJson(reader, PoseMock.class);
                if (pose == null || pose.getPosition() == null || pose.getRotation() == null) {
                    throw new IOException("Invalid pose payload from localization server");
                }
                return pose;
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
