package com.example.digibook_examen1_1198109;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.digibook_examen1_1198109.databinding.FragmentOnboarding3Binding;

public class onboarding3Fragment extends Fragment {

    private FragmentOnboarding3Binding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboarding3Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Terminar (ir a Login)
        binding.buttonNext.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_onboarding3Fragment_to_loginFragment);
        });

        // Ir Atrás
        binding.buttonBack.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_onboarding3Fragment_to_onboarding2Fragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
