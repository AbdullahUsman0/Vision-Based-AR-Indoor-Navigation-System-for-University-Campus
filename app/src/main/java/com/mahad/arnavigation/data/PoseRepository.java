package com.mahad.arnavigation.data;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class PoseRepository {
    private final Context context;
    private final Gson gson;

    public PoseRepository(Context context) {
        this(context, new Gson());
    }

    public PoseRepository(Context context, Gson gson) {
        this.context = context;
        this.gson = gson;
    }

    public PoseMock loadMockPose() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(context.getAssets().open("pose_mock.json"), StandardCharsets.UTF_8)
        )) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            return gson.fromJson(jsonBuilder.toString(), PoseMock.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load pose_mock.json", e);
        }
    }
}
