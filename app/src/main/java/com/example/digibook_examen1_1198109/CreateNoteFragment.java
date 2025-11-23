package com.example.digibook_examen1_1198109;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.slider.Slider;

public class CreateNoteFragment extends Fragment {

    private DrawingView drawingView;
    private FrameLayout drawingContainer;
    private EditText lastFocusedEditText; // Para saber a qué texto cambiarle el tamaño

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        drawingView = view.findViewById(R.id.drawingView);
        drawingContainer = view.findViewById(R.id.drawingContainer);
        Slider sliderSize = view.findViewById(R.id.sliderSize);

        // Configurar botones de colores
        view.findViewById(R.id.btnBlack).setOnClickListener(v -> drawingView.setColor("#000000"));
        view.findViewById(R.id.btnRed).setOnClickListener(v -> drawingView.setColor("#FF0000"));
        view.findViewById(R.id.btnBlue).setOnClickListener(v -> drawingView.setColor("#0000FF"));

        // Configurar Slider de tamaño
        sliderSize.addOnChangeListener((slider, value, fromUser) -> {
            // Si hay una caja de texto con foco, cambiamos el tamaño de su letra
            if (lastFocusedEditText != null && lastFocusedEditText.hasFocus()) {
                lastFocusedEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
            } else {
                // Si no, cambiamos el tamaño del pincel
                drawingView.setBrushSize((int) value);
            }
        });

        // Botón para agregar caja de texto
        view.findViewById(R.id.btnAddText).setOnClickListener(v -> addTextBox());
    }

    private void addTextBox() {
        EditText editText = new EditText(requireContext());
        editText.setHint("Escribe aquí...");
        editText.setBackgroundColor(Color.TRANSPARENT); // Fondo transparente
        editText.setTextSize(20);

        // LayoutParams para posicionarlo inicialmente en el centro
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = 100; // Posición inicial X
        params.topMargin = 100;  // Posición inicial Y
        editText.setLayoutParams(params);

        // Listener para guardar referencia cuando se toca
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                lastFocusedEditText = (EditText) v;
            }
        });

        // Lógica simple para arrastrar (Drag and Drop básico)
        editText.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // Permitir editar si es un click simple, arrastrar si es movimiento
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        lastFocusedEditText = (EditText) view; // Actualizar referencia
                        view.requestFocus();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                }
                return false; // Retornar false permite que el evento de click/foco siga funcionando
            }
        });

        drawingContainer.addView(editText);
        editText.requestFocus(); // Dar foco inmediato
        lastFocusedEditText = editText;
    }
}