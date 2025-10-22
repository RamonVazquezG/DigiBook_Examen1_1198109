package com.example.digibook_examen1_1198109;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavOptions;

import com.example.digibook_examen1_1198109.databinding.FragmentDashboardBinding;

public class dashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Inicializar los ActivityResultLaunchers ---
        // Deben registrarse en onCreate() o onAttach() del Fragment.

        // 1. Launcher para solicitar permiso de CÁMARA
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(getContext(), R.string.permission_camera_denied, Toast.LENGTH_LONG).show();
            }
        });

        // 2. Launcher para el resultado de la CÁMARA
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    binding.imageUserProfile.setImageBitmap(imageBitmap);
                }
            }
        });

        // 3. Launcher para solicitar permiso de ALMACENAMIENTO
        requestStoragePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                dispatchOpenPdfIntent();
            } else {
                Toast.makeText(getContext(), R.string.permission_storage_denied, Toast.LENGTH_LONG).show();
            }
        });

        // 4. Launcher para el resultado del SELECTOR DE PDF
        pdfPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                Toast.makeText(getContext(), "PDF seleccionado: " + uri.getPath(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Recuperar y mostrar el nombre de usuario de los argumentos
        if (getArguments() != null) {
            String username = getArguments().getString("USERNAME_EXTRA", "Usuario");
            binding.textUsername.setText(username);

            // Opcional: Mostrar el nombre de usuario en la ActionBar
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle("Perfil de " + username);
        }

        // 2. Configurar listeners para los botones
        setupClickListeners();

        // 3. Añadir el proveedor de menú (para el botón de logout)
        setupMenu();
    }

    private void setupClickListeners() {
        // --- Acción: Tomar foto ---
        binding.imageUserProfile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // --- Acción: Abrir selector de PDF ---
        binding.buttonLastNotebook.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                dispatchOpenPdfIntent();
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });

        // --- Acción: Abrir app de notas ---
        binding.buttonNewNotebook.setOnClickListener(v -> {
            dispatchNewNoteIntent();
        });
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.dashboard_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_logout) {
                    logout();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    /**
     * Cierra la sesión y regresa al LoginFragment, limpiando la pila.
     */
    private void logout() {
        NavController navController = Navigation.findNavController(requireView());

        // Opciones para limpiar toda la pila de navegación
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true) // Popea hasta el inicio del grafo
                .build();

        navController.navigate(R.id.action_dashboardFragment_to_loginFragment, null, navOptions);
    }

    // --- Métodos de Intents (copiados de la actividad anterior) ---

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), R.string.no_camera_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchOpenPdfIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            pdfPickerLauncher.launch(Intent.createChooser(intent, "Selecciona un PDF"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), R.string.no_pdf_picker, Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchNewNoteIntent() {
        Intent intent = new Intent(Intent.ACTION_CREATE_NOTE);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), R.string.no_note_app, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        // Opcional: Restaurar título de la ActionBar
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(getString(R.string.app_name));
    }
}
