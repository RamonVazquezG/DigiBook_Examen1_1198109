package com.example.digibook_examen1_1198109;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
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

import com.example.digibook_examen1_1198109.databinding.FragmentDashboardBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class dashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private UserManager userManager;
    private NetworkReceiver networkReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = new UserManager(requireContext());

        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(getContext(), R.string.permission_camera_denied, Toast.LENGTH_LONG).show();
            }
        });

        // Modificado para guardar la imagen en almacenamiento interno
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        // Guardar imagen en almacenamiento interno y obtener ruta
                        String path = saveImageToInternalStorage(imageBitmap);
                        if (path != null) {
                            // Guardar ruta en SharedPreferences para el usuario actual
                            userManager.saveUserPhoto(path);
                            // Mostrar imagen
                            if (binding != null) binding.imageUserProfile.setImageBitmap(imageBitmap);
                        }
                    }
                }
            }
        });

        requestStoragePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                dispatchOpenPdfIntent();
            } else {
                showPermissionDeniedDialog();
            }
        });

        requestNotificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (!isGranted) {
                Toast.makeText(getContext(), "Sin permiso, la alarma no mostrará notificación visual.", Toast.LENGTH_SHORT).show();
            }
        });

        pdfPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                Toast.makeText(getContext(), "PDF seleccionado: " + (uri != null ? uri.getPath() : "?"), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    // Configura la UI y carga datos del usuario
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configurar el nombre de usuario en la UI y la ActionBar
        if (getArguments() != null) {
            String username = getArguments().getString("USERNAME_EXTRA", "Usuario");
            if (binding != null) binding.textUsername.setText(username);
            if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Perfil de " + username);
            }
        }

        // Cargar los datos persistentes del usuario
        loadUserData();

        setupClickListeners();
        setupMenu();
        checkNotificationPermission();
    }

    // Carga la foto y la configuración de alarma del usuario actual.
    private void loadUserData() {
        // 1. Cargar Foto
        String photoPath = userManager.getUserPhoto();
        if (photoPath != null && !photoPath.isEmpty()) {
            File imgFile = new File(photoPath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                binding.imageUserProfile.setImageBitmap(myBitmap);
            }
        } else {
            // Resetear a icono por defecto si no hay foto
            binding.imageUserProfile.setImageResource(android.R.drawable.ic_menu_camera);
        }

        // 2. Cargar Alarma
        String alarmTime = userManager.getUserAlarm();
        if (alarmTime != null && !alarmTime.isEmpty()) {
            binding.textAlarmStatus.setText("Alarma: " + alarmTime);
        } else {
            binding.textAlarmStatus.setText("Sin alarma activa");
        }
    }

    // Guarda el bitmap en el almacenamiento interno de la app.
    private String saveImageToInternalStorage(Bitmap bitmapImage) {
        // Usamos el email del usuario para crear un nombre único de archivo
        String email = userManager.getCurrentUserEmail();
        // Sanitizamos el email para usarlo como nombre de archivo
        String fileName = "profile_" + email.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";

        // Guardar en el directorio interno de la app
        File directory = requireContext().getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, fileName);

        // Escribir el bitmap en el archivo
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            return mypath.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Configura los listeners para los botones de la UI
    private void setupClickListeners() {
        if (binding == null) return;

        // Manejo de permisos de cámara y captura de foto
        binding.imageUserProfile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Manejo de permisos de almacenamiento según versión de Android
        binding.buttonLastNotebook.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    dispatchOpenPdfIntent();
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showPermissionRationaleDialog();
                } else {
                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            } else {
                dispatchOpenPdfIntent();
            }
        });

        // Nuevo Intent para crear nota de texto
        binding.buttonNewNotebook.setOnClickListener(v -> dispatchNewNoteIntent());

        // Configuración de alarma
        binding.buttonSetAlarm.setOnClickListener(v -> showTimePickerDialog());

        // Listener para Cita
        binding.buttonOpenQuote.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_quoteFragment);
        });
    }

    // Muestra un diálogo explicando por qué se necesita el permiso de almacenamiento
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.permission_storage_rationale_title)
                .setMessage(R.string.permission_storage_rationale_msg)
                .setPositiveButton(R.string.accept, (dialog, which) -> {
                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // Muestra un diálogo indicando que el permiso fue denegado permanentemente
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.permission_denied)
                .setMessage("Esta funcionalidad requiere acceso al almacenamiento. Por favor habilita el permiso en Configuración.")
                .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    // Verifica y solicita permiso de notificaciones si es necesario
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // Muestra el diálogo del selector de hora para la alarma
    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Crear y mostrar el TimePickerDialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute1) -> setAlarm(hourOfDay, minute1),
                hour, minute, true);
        timePickerDialog.show();
    }

    // Configura la alarma diaria a la hora seleccionada
    private void setAlarm(int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);

        // Crear PendingIntent para la alarma
        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Configurar la hora de la alarma
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Si la hora ya pasó hoy, programar para mañana
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        // Programar la alarma
        if (alarmManager != null) {
            try {
                // Usar setExactAndAllowWhileIdle para mayor precisión si el permiso está concedido
                // O simplemente setInexactRepeating como pide el examen básico
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pendingIntent);

                // Guardar persistencia
                String timeString = String.format("%02d:%02d", hour, minute);
                userManager.saveUserAlarm(timeString);

                // Actualizar UI
                binding.textAlarmStatus.setText(String.format(getString(R.string.alarm_set_format), hour, minute));
                Toast.makeText(requireContext(), getString(R.string.alarm_scheduled, hour, minute), Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(requireContext(), "No se pudo programar la alarma exacta (falta permiso)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Inicia el Intent para capturar una foto con la cámara
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (ActivityNotFoundException e) {
            if (getContext() != null) Toast.makeText(getContext(), R.string.no_camera_app, Toast.LENGTH_SHORT).show();
        }
    }

    // Inicia el Intent para seleccionar un PDF desde el almacenamiento
    private void dispatchOpenPdfIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Lanzar el selector de PDF
        try {
            pdfPickerLauncher.launch(Intent.createChooser(intent, "Selecciona un PDF"));
        } catch (ActivityNotFoundException e) {
            if (getContext() != null) Toast.makeText(getContext(), R.string.no_pdf_picker, Toast.LENGTH_SHORT).show();
        }
    }

    // Inicia el Intent para crear una nueva nota de texto
    private void dispatchNewNoteIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        // Lanzar el chooser para crear nota
        try {
            startActivity(Intent.createChooser(intent, "Crear nota con..."));
        } catch (ActivityNotFoundException e) {
            if (getContext() != null) Toast.makeText(getContext(), R.string.no_note_app, Toast.LENGTH_SHORT).show();
        }
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

    // Maneja el cierre de sesión del usuario
    private void logout() {
        if (getView() == null) return;

        userManager.logout();

        NavController navController = Navigation.findNavController(requireView());
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        navController.navigate(R.id.action_dashboardFragment_to_loginFragment, null, navOptions);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.app_name));
        }
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Registrar BroadcastReceiver dinámicamente para cambios en tiempo real
        networkReceiver = new NetworkReceiver(binding.getRoot(), isConnected -> {
            if (binding != null) {
                if (isConnected) {
                    binding.textApiStatus.setText("Toca para ver una frase motivacional");
                    binding.buttonOpenQuote.setEnabled(true);
                    binding.buttonOpenQuote.setText("Ver Cita");
                } else {
                    binding.textApiStatus.setText("Sin conexión para citas");
                    binding.buttonOpenQuote.setEnabled(false);
                    binding.buttonOpenQuote.setText("Offline");
                }
            }
        });
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        requireActivity().registerReceiver(networkReceiver, filter);
    }

    // Desregistrar el BroadcastReceiver al pausar el fragmento
    @Override
    public void onPause() {
        super.onPause();
        if (networkReceiver != null) {
            try {
                requireActivity().unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
                // Ya estaba desregistrado
            }
        }
    }


}