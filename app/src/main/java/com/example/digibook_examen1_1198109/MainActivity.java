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

        // Configuración de EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Obtener el NavHostFragment usando el ID del FragmentContainerView.
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        // Obtener el NavController DESDE el NavHostFragment.
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Configurar la ActionBar con el NavController.
            NavigationUI.setupActionBarWithNavController(this, navController);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp()) || super.onSupportNavigateUp();
    }
}

