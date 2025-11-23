package com.example.digibook_examen1_1198109;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.digibook_examen1_1198109.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    // Binding generado por ViewBinding para acceder a las vistas del layout
    private FragmentRegisterBinding binding;
    // Gestor de usuarios que encapsula la lógica de registro (por ejemplo SharedPreferences)
    private UserManager userManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla el layout usando ViewBinding, inicializa el UserManager y devuelve la vista raíz.
        // Aquí se prepara la UI y las dependencias necesarias antes de que se muestre el fragmento.
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        userManager = new UserManager(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configura los listeners de la interfaz. En este caso, al pulsar el botón de registro
        // se ejecuta la función registerUser pasando la vista para poder navegar después.
        binding.buttonRegister.setOnClickListener(v -> registerUser(v));
    }

    // Lee los campos del formulario, valida que no estén vacíos, intenta registrar el usuario
    // mediante userManager y muestra toasts según el resultado. Si el registro es exitoso,
    // navega hacia atrás (regresa al login).
    private void registerUser(View view) {
        String username = binding.editTextRegUsername.getText().toString().trim();
        String email = binding.editTextRegEmail.getText().toString().trim();
        String password = binding.editTextRegPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), R.string.empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // Registrar usuario en SharedPreferences
        boolean success = userManager.registerUser(username, email, password);

        if (success) {
            Toast.makeText(getContext(), R.string.register_success, Toast.LENGTH_SHORT).show();
            // Regresar al login
            Navigation.findNavController(view).navigateUp();
        } else {
            Toast.makeText(getContext(), R.string.register_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Evita fugas de memoria liberando la referencia al binding cuando la vista se destruye
        binding = null;
    }
}