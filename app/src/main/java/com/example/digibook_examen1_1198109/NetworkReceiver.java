package com.example.digibook_examen1_1198109;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class NetworkReceiver extends BroadcastReceiver {

    // Interfaz para comunicar el estado a la actividad
    public interface NetworkListener {
        void onNetworkChanged(boolean isConnected);
    }

    private NetworkListener listener;
    private View viewForSnackbar;

    public NetworkReceiver() {
        // Constructor vacío requerido por el manifiesto
    }

    // Constructor con parámetros para la actividad
    public NetworkReceiver(View viewForSnackbar, NetworkListener listener) {
        this.viewForSnackbar = viewForSnackbar;
        this.listener = listener;
    }

    // Método que se llama cuando cambia el estado de la red
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        // 1. Notificar al listener para actualizar UI
        if (listener != null) {
            listener.onNetworkChanged(isConnected);
        }

        // 2. Mostrar Snackbar si no hay conexión
        if (!isConnected && viewForSnackbar != null) {
            Snackbar.make(viewForSnackbar, "¡Sin conexión a internet! La inspiración diaria no estará disponible.", Snackbar.LENGTH_LONG)
                    .setAction("OK", v -> {}) // Botón para cerrar
                    .show();
        }
    }
}