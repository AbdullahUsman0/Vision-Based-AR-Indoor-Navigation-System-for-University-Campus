package com.mahad.arnavigation.ar;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ar.sceneform.ux.ArFragment;

public class NavigationArFragment extends ArFragment {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getArSceneView().getPlaneRenderer().setVisible(false);
    }
}
