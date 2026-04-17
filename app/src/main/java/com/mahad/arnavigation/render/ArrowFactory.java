package com.mahad.arnavigation.render;

import android.content.Context;
import android.util.Log;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ShapeFactory;

import java.util.function.Consumer;

public final class ArrowFactory {
    private static final String TAG = "ArrowFactory";

    private ArrowFactory() {
    }

    public static void createArrowNode(Context context, Consumer<Node> onReady) {
        MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.RED))
            .thenAccept(material -> {
                Node root = new Node();

                Node shaft = new Node();
                shaft.setRenderable(ShapeFactory.makeCube(
                    new Vector3(0.04f, 0.04f, 0.5f),
                    new Vector3(0f, 0f, 0f),
                    material
                ));

                Node head = new Node();
                head.setRenderable(ShapeFactory.makeCube(
                    new Vector3(0.10f, 0.06f, 0.16f),
                    new Vector3(0f, 0f, 0f),
                    material
                ));
                head.setLocalPosition(new Vector3(0f, 0f, -0.32f));

                shaft.setParent(root);
                head.setParent(root);
                onReady.accept(root);
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Failed to create arrow renderable", throwable);
                return null;
            });
    }
}
