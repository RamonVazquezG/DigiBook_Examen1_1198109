package com.example.digibook_examen1_1198109;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.digibook_examen1_1198109.databinding.FragmentDashboardBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class dashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    private UserManager userManager;
    private NetworkReceiver networkReceiver;

    // Variables para las notas
    private NoteRepository noteRepository;
    private NotesAdapter notesAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = new UserManager(requireContext());
        noteRepository = new NoteRepository(requireContext());

        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(getContext(), R.string.permission_camera_denied, Toast.LENGTH_LONG).show();
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        String path = saveImageToInternalStorage(imageBitmap);
                        if (path != null) {
                            userManager.saveUserPhoto(path);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String username = getArguments().getString("USERNAME_EXTRA", "Usuario");
            if (binding != null) binding.textUsername.setText(username);
            if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Perfil de " + username);
            }
        }

        loadUserData();
        setupClickListeners();
        setupMenu();
        checkNotificationPermission();

        // Configurar la lista de notas
        setupNotesList();
    }

    private void setupNotesList() {
        // Deshabilitar scroll anidado para que funcione bien dentro del NestedScrollView
        binding.recyclerNotes.setNestedScrollingEnabled(false);
        binding.recyclerNotes.setLayoutManager(new LinearLayoutManager(getContext()));

        // Configurar el adaptador con los listeners de clic y borrado
        notesAdapter = new NotesAdapter(noteRepository.getSavedNotes(), new NotesAdapter.OnNoteInteractionListener() {
            @Override
            public void onNoteClick(String noteName) {
                openNote(noteName);
            }

            @Override
            public void onNoteDelete(String noteName) {
                showDeleteConfirmationDialog(noteName);
            }
        });
        binding.recyclerNotes.setAdapter(notesAdapter);
    }

    private void openNote(String noteName) {
        Bundle bundle = new Bundle();
        bundle.putString("noteName", noteName);
        Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_createNote, bundle);
    }

    private void showDeleteConfirmationDialog(String noteName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Nota")
                .setMessage("¿Estás seguro de que quieres borrar \"" + noteName + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean deleted = noteRepository.deleteNote(noteName);
                    if (deleted) {
                        Toast.makeText(getContext(), "Nota eliminada", Toast.LENGTH_SHORT).show();
                        refreshNotesList();
                    } else {
                        Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void refreshNotesList() {
        if (notesAdapter != null && noteRepository != null) {
            notesAdapter.updateData(noteRepository.getSavedNotes());
        }
    }

    private void loadUserData() {
        String photoPath = userManager.getUserPhoto();
        if (photoPath != null && !photoPath.isEmpty()) {
            File imgFile = new File(photoPath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                binding.imageUserProfile.setImageBitmap(myBitmap);
            }
        } else {
            binding.imageUserProfile.setImageResource(android.R.drawable.ic_menu_camera);
        }

        String alarmTime = userManager.getUserAlarm();
        if (alarmTime != null && !alarmTime.isEmpty()) {
            binding.textAlarmStatus.setText("Alarma: " + alarmTime);
        } else {
            binding.textAlarmStatus.setText("Sin alarma activa");
        }
    }

    private String saveImageToInternalStorage(Bitmap bitmapImage) {
        String email = userManager.getCurrentUserEmail();
        String fileName = "profile_" + email.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
        File directory = requireContext().getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, fileName);
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

    private void setupClickListeners() {
        if (binding == null) return;

        binding.imageUserProfile.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

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

        binding.buttonNewNotebook.setOnClickListener(v -> showCreateNoteDialog(v));

        binding.buttonSetAlarm.setOnClickListener(v -> showTimePickerDialog());

        binding.buttonOpenQuote.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_quoteFragment);
        });
    }

    private void showCreateNoteDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nueva Nota Digital");
        builder.setMessage("Ingresa un nombre para tu nota:");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Crear", (dialog, which) -> {
            String noteName = input.getText().toString().trim();
            if (!noteName.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putString("noteName", noteName);
                Navigation.findNavController(view).navigate(R.id.action_dashboard_to_createNote, bundle);
            } else {
                Toast.makeText(getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

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

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute1) -> setAlarm(hourOfDay, minute1),
                hour, minute, true);
        timePickerDialog.show();
    }

    private void setAlarm(int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        if (alarmManager != null) {
            try {
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pendingIntent);
                String timeString = String.format("%02d:%02d", hour, minute);
                userManager.saveUserAlarm(timeString);
                binding.textAlarmStatus.setText(String.format(getString(R.string.alarm_set_format), hour, minute));
                Toast.makeText(requireContext(), getString(R.string.alarm_scheduled, hour, minute), Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(requireContext(), "No se pudo programar la alarma exacta (falta permiso)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (ActivityNotFoundException e) {
            if (getContext() != null) Toast.makeText(getContext(), R.string.no_camera_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchOpenPdfIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            pdfPickerLauncher.launch(Intent.createChooser(intent, "Selecciona un PDF"));
        } catch (ActivityNotFoundException e) {
            if (getContext() != null) Toast.makeText(getContext(), R.string.no_pdf_picker, Toast.LENGTH_SHORT).show();
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

        refreshNotesList();

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

    @Override
    public void onPause() {
        super.onPause();
        if (networkReceiver != null) {
            try {
                requireActivity().unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
            }
        }
    }
}