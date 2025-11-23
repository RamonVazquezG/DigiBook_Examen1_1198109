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
    private UserManager userManager;

    // Infla el layout y prepara UserManager
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        userManager = new UserManager(requireContext()); // Inicializar UserManager
        return binding.getRoot();
    }

    // Configura listeners para botones
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Listener para Login
        binding.buttonLogin.setOnClickListener(v -> validateLogin(v));

        // Listener para ir a Registro
        binding.buttonGoToRegister.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment);
        });
    }

    // Valida campos y realiza login
    private void validateLogin(View view) {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        // Validar campos vacíos
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), R.string.empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // Usamos el UserManager para verificar credenciales en SharedPreferences
        String username = userManager.loginUser(email, password);

        if (username != null) {
            // Login exitoso
            Toast.makeText(getContext(), R.string.login_success, Toast.LENGTH_SHORT).show();
            navigateToDashboard(view, username);
        } else {
            // Login fallido
            Toast.makeText(getContext(), R.string.login_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToDashboard(View view, String username) {
        NavController navController = Navigation.findNavController(view);
        Bundle bundle = new Bundle();
        bundle.putString("USERNAME_EXTRA", username);
        navController.navigate(R.id.action_loginFragment_to_dashboardFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}