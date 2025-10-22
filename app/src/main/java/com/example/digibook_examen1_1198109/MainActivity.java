package com.example.digibook_examen1_1198109;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
// Import NavHostFragment
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // Carga el layout con el NavHost

        // Configuración de EdgeToEdge (de tu plantilla)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- SOLUCIÓN: Obtener NavController del NavHostFragment ---
        // 1. Obtener el NavHostFragment usando el ID del FragmentContainerView.
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        // 2. Obtener el NavController DESDE el NavHostFragment.
        //    Esto asegura que el NavController ya existe cuando lo pides.
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // 3. Configurar la ActionBar con el NavController obtenido.
            NavigationUI.setupActionBarWithNavController(this, navController);
        } else {
            // Manejar el caso improbable de que el fragmento no se encuentre
            // (esto indicaría un problema grave en el layout o configuración)
            // Log.e("MainActivity", "NavHostFragment not found!");
        }
    }

    // Manejar el evento de clic en el botón "Up" (Atrás)
    @Override
    public boolean onSupportNavigateUp() {
        // Asegurarse de que navController no sea null antes de usarlo
        return (navController != null && navController.navigateUp()) || super.onSupportNavigateUp();
    }
}

