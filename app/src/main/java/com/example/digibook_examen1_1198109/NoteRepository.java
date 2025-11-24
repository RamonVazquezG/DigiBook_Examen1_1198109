package com.example.digibook_examen1_1198109;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class NoteRepository {

    private Context context;

    public NoteRepository(Context context) {
        this.context = context;
    }

    // Guarda el contenido JSON
    public void saveNote(String noteName, String jsonData) {
        String fileName = noteName + ".json";
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(jsonData.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Carga el contenido JSON
    public String loadNote(String noteName) {
        String fileName = noteName + ".json";
        File file = new File(context.getFilesDir(), fileName);
        if (!file.exists()) {
            return null;
        }

        try {
            FileInputStream fis = context.openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            fis.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // NUEVO: Obtener la lista de nombres de notas guardadas
    public List<String> getSavedNotes() {
        List<String> notes = new ArrayList<>();
        File dir = context.getFilesDir();
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    // Quitamos la extensión .json para mostrar solo el nombre
                    String name = file.getName().replace(".json", "");
                    notes.add(name);
                }
            }
        }
        return notes;
    }
}