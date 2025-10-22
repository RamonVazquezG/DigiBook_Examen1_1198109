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
import androidx.navigation.NavOptions;

import com.example.digibook_examen1_1198109.R;
import com.example.digibook_examen1_1198109.databinding.FragmentDashboardBinding;

public class dashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Launcher para solicitar permiso de camara
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(getContext(), R.string.permission_camera_denied, Toast.LENGTH_LONG).show();
            }
        });

        // Launcher para el resultado de la camara
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (binding != null) {
                        binding.imageUserProfile.setImageBitmap(imageBitmap);
                    }
                }
            }
        });


        // Launcher para el resultado del selector de pdf
        pdfPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                Toast.makeText(getContext(), "PDF seleccionado: " + (uri != null ? uri.getPath() : "Error"), Toast.LENGTH_LONG).show();
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

        // Recuperar y mostrar el nombre de usuario
        if (getArguments() != null) {
            String username = getArguments().getString("USERNAME_EXTRA", "Usuario");
            if (binding != null) { // Check binding is not null
                binding.textUsername.setText(username);
            }
            // Actualizar ActionBar
            if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Perfil de " + username);
            }
        }

        setupClickListeners();

        setupMenu();
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.imageUserProfile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                // solicita el permiso
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.buttonLastNotebook.setOnClickListener(v -> {
            // Llama directamente al método que lanza el Intent
            dispatchOpenPdfIntent();
        });

        // Abrir app de notas
        binding.buttonNewNotebook.setOnClickListener(v -> {
            dispatchNewNoteIntent();
        });
    }

    private void setupMenu() {
        if (getActivity() == null || getViewLifecycleOwner() == null) return;

        getActivity().addMenuProvider(new MenuProvider() {
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

    private void logout() {
        if (getView() == null) return;
        NavController navController = Navigation.findNavController(requireView());
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        navController.navigate(R.id.action_dashboardFragment_to_loginFragment, null, navOptions);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (ActivityNotFoundException e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.no_camera_app, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchOpenPdfIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            pdfPickerLauncher.launch(Intent.createChooser(intent, "Selecciona un PDF"));
        } catch (ActivityNotFoundException e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.no_pdf_picker, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchNewNoteIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        try {
            startActivity(Intent.createChooser(intent, "Crear nota con..."));
        } catch (ActivityNotFoundException e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.no_note_app, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.app_name));
        }
        binding = null;
    }
}

