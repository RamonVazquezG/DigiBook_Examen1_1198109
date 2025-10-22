package com.example.digibook_examen1_1198109;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.digibook_examen1_1198109.databinding.FragmentOnboarding2Binding;

public class onboarding2Fragment extends Fragment {

    private FragmentOnboarding2Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboarding2Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ir al Siguiente
        binding.buttonNext.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_onboarding2Fragment_to_onboarding3Fragment);
        });

        // Ir Atrás
        binding.buttonBack.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_onboarding2Fragment_to_onboarding1Fragment);
        });

        // Omitir
        binding.buttonSkip.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_onboarding2Fragment_to_loginFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
