package com.example.digibook_examen1_1198109;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;

public class UserManager {
    private static final String PREF_NAME = "DigiBookUsers";
    private static final String KEY_CURRENT_USER = "current_user";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public UserManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public boolean registerUser(String username, String email, String password) {
        // Verifica si el correo electrónico ya existe
        if (sharedPreferences.contains(email)) {
            return false;
        }
        try {
            // Guarda los datos del usuario en formato JSON
            JSONObject userJson = new JSONObject();
            userJson.put("username", username);
            userJson.put("email", email);
            userJson.put("password", password);
            // Inicializamos campos vacíos para foto y alarma
            userJson.put("photoUri", "");
            userJson.put("alarmTime", "");

            editor.putString(email, userJson.toString());
            editor.apply();
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Método para el inicio de sesión
    public String loginUser(String email, String password) {
        // Verifica si el correo electrónico existe
        String userString = sharedPreferences.getString(email, null);
        if (userString == null) return null;

        // Verifica si la contraseña coincide
        try {
            JSONObject userJson = new JSONObject(userString);
            String storedPassword = userJson.getString("password");
            if (storedPassword.equals(password)) {
                // Guardamos el email como identificador de la sesión actual
                // para poder buscar y actualizar sus datos después
                editor.putString(KEY_CURRENT_USER, email);
                editor.apply();
                return userJson.getString("username");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Método para cerrar sesión
    public void logout() {
        editor.remove(KEY_CURRENT_USER);
        editor.apply();
    }

    // Obtener el email del usuario actual (clave principal)
    public String getCurrentUserEmail() {
        return sharedPreferences.getString(KEY_CURRENT_USER, null);
    }

    // Guardar URI de la foto
    public void saveUserPhoto(String photoUri) {
        // Obtener el email del usuario actual
        String email = getCurrentUserEmail();
        if (email == null) return;

        // Actualizar el campo de la foto en el JSON del usuario
        try {
            String userString = sharedPreferences.getString(email, null);
            if (userString != null) {
                JSONObject userJson = new JSONObject(userString);
                userJson.put("photoUri", photoUri);
                editor.putString(email, userJson.toString());
                editor.apply();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Obtener URI de la foto
    public String getUserPhoto() {
        // Obtener el email del usuario actual
        String email = getCurrentUserEmail();
        if (email == null) return null;

        // Obtener el campo de la foto del JSON del usuario
        try {
            String userString = sharedPreferences.getString(email, null);
            if (userString != null) {
                JSONObject userJson = new JSONObject(userString);
                return userJson.optString("photoUri", null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Guardar hora de alarma (formato "HH:mm")
    public void saveUserAlarm(String alarmTime) {
        // Obtener el email del usuario actual
        String email = getCurrentUserEmail();
        if (email == null) return;

        // Actualizar el campo de la alarma en el JSON del usuario
        try {
            String userString = sharedPreferences.getString(email, null);
            if (userString != null) {
                JSONObject userJson = new JSONObject(userString);
                userJson.put("alarmTime", alarmTime);
                editor.putString(email, userJson.toString());
                editor.apply();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Obtener hora de alarma
    public String getUserAlarm() {
        String email = getCurrentUserEmail();
        if (email == null) return null;

        // Obtener el campo de la alarma del JSON del usuario
        try {
            String userString = sharedPreferences.getString(email, null);
            if (userString != null) {
                JSONObject userJson = new JSONObject(userString);
                return userJson.optString("alarmTime", null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}