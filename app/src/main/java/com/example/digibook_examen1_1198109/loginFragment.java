package com.example.digibook_examen1_1198109;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.digibook_examen1_1198109.databinding.FragmentLoginBinding;

public class loginFragment extends Fragment {

    private FragmentLoginBinding binding;

    // Credenciales simuladas
    private final String VALID_USERNAME = "admin";
    private final String VALID_PASSWORD = "1234";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Listener para el botón de Login
        binding.buttonLogin.setOnClickListener(v -> validateLogin(v));
    }

    /**
     * Valida las credenciales ingresadas por el usuario.
     */
    private void validateLogin(View view) {
        String username = binding.editTextUsername.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (username.equals(VALID_USERNAME) && password.equals(VALID_PASSWORD)) {
            // Credenciales correctas
            Toast.makeText(getContext(), R.string.login_success, Toast.LENGTH_SHORT).show();
            navigateToDashboard(view, username);
        } else {
            // Credenciales incorrectas
            Toast.makeText(getContext(), R.string.login_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navega a DashboardFragment, pasando el nombre de usuario.
     *
     * @param view     La vista actual (para encontrar el NavController).
     * @param username El nombre de usuario validado.
     */
    private void navigateToDashboard(View view, String username) {
        NavController navController = Navigation.findNavController(view);

        // Pasamos el nombre de usuario al Dashboard usando un Bundle
        Bundle bundle = new Bundle();
        bundle.putString("USERNAME_EXTRA", username);

        // Navegamos usando la acción definida en nav_graph.xml
        navController.navigate(R.id.action_loginFragment_to_dashboardFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
