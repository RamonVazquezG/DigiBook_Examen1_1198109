package com.example.digibook_examen1_1198109;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// Loader para cargar citas de forma asíncrona
public class QuoteLoader extends AsyncTaskLoader<String> {

    // URL de la API de citas
    private static final String API_URL = "https://zenquotes.io/api/random";

    // Constructor
    public QuoteLoader(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        // Forzar la carga de datos
        forceLoad();
    }

    // Cargar la cita en segundo plano
    @Nullable
    @Override
    public String loadInBackground() {
        try {
            // Conectar a la API
            URL url = new URL(API_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Leer la respuesta
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            // Parsear JSON (ZenQuotes devuelve un array con un objeto)
            String jsonResponse = stringBuilder.toString();
            JSONArray jsonArray = new JSONArray(jsonResponse);
            JSONObject jsonObject = jsonArray.getJSONObject(0);

            // Formatear: "Frase" - Autor
            String quote = jsonObject.getString("q");
            String author = jsonObject.getString("a");

            return "\"" + quote + "\"\n\n- " + author;

        } catch (Exception e) {
            e.printStackTrace();
            return null; // Error
        }
    }
}