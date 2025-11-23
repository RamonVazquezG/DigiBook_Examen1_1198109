package com.example.digibook_examen1_1198109;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

public class CreateNoteFragment extends Fragment {

    private DrawingView drawingView;
    private FrameLayout textContainer;
    private LinearLayout brushControls, textControls, colorPalette;
    private MaterialButton btnModeDraw, btnModeText, btnAddText;

    private EditText activeEditText; // La caja de texto seleccionada
    private boolean isDrawMode = true;
    private int currentColor = Color.BLACK;

    private final String[] colors = {
            "#000000", "#545454", "#9E9E9E", "#F44336",
            "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688",
            "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B",
            "#FFC107", "#FF9800", "#FF5722", "#795548"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Referencias UI
        drawingView = view.findViewById(R.id.drawingView);
        textContainer = view.findViewById(R.id.textContainer);
        brushControls = view.findViewById(R.id.brushControls);
        textControls = view.findViewById(R.id.textControls);
        colorPalette = view.findViewById(R.id.colorPalette);

        btnModeDraw = view.findViewById(R.id.btnModeDraw);
        btnModeText = view.findViewById(R.id.btnModeText);
        btnAddText = view.findViewById(R.id.btnAddText);

        Slider sliderBrush = view.findViewById(R.id.sliderBrushSize);
        Slider sliderText = view.findViewById(R.id.sliderTextSize);

        // 1. Configurar Modos
        btnModeDraw.setOnClickListener(v -> setMode(true));
        btnModeText.setOnClickListener(v -> setMode(false));
        btnAddText.setOnClickListener(v -> addDraggableText());

        // 2. Configurar Slider Pincel
        sliderBrush.addOnChangeListener((slider, value, fromUser) -> {
            drawingView.setBrushSize((int) value);
        });

        // 3. Configurar Slider Texto
        sliderText.addOnChangeListener((slider, value, fromUser) -> {
            if (activeEditText != null) {
                activeEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
            }
        });

        // 4. Crear Paleta de Colores
        createColorButtons();

        // Iniciar en modo dibujo
        setMode(true);
    }

    private void setMode(boolean draw) {
        isDrawMode = draw;
        drawingView.setDrawingEnabled(draw);

        if (draw) {
            // UI Modo Dibujo
            brushControls.setVisibility(View.VISIBLE);
            textControls.setVisibility(View.GONE);
            btnAddText.setVisibility(View.GONE);

            // Estilos botones
            updateButtonStyle(btnModeDraw, true);
            updateButtonStyle(btnModeText, false);

            // Quitar foco del texto para evitar teclado
            if (activeEditText != null) activeEditText.clearFocus();

        } else {
            // UI Modo Texto
            brushControls.setVisibility(View.GONE);
            textControls.setVisibility(View.VISIBLE);
            btnAddText.setVisibility(View.VISIBLE);

            updateButtonStyle(btnModeDraw, false);
            updateButtonStyle(btnModeText, true);
        }
    }

    private void updateButtonStyle(MaterialButton btn, boolean active) {
        if (active) {
            btn.setStrokeWidth(4);
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(com.google.android.material.R.color.design_default_color_primary)));
        } else {
            btn.setStrokeWidth(0);
        }
    }

    private void createColorButtons() {
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        for (String colorHex : colors) {
            View colorView = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            colorView.setLayoutParams(params);

            // Fondo circular de color
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(Color.parseColor(colorHex));
            shape.setStroke(2, Color.LTGRAY);
            colorView.setBackground(shape);

            colorView.setOnClickListener(v -> {
                currentColor = Color.parseColor(colorHex);
                if (isDrawMode) {
                    drawingView.setColor(colorHex);
                } else if (activeEditText != null) {
                    activeEditText.setTextColor(currentColor);
                }
            });

            colorPalette.addView(colorView);
        }
    }

    private void addDraggableText() {
        EditText et = new EditText(requireContext());
        et.setText("Texto");
        et.setTextColor(currentColor);
        et.setTextSize(24); // Tamaño inicial
        et.setBackgroundColor(Color.TRANSPARENT);
        et.setPadding(16, 16, 16, 16);

        // Posición inicial en el centro
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        et.setLayoutParams(params);

        // Lógica de Arrastre (Touch Listener)
        et.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            long startClickTime;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // Solo permitir mover si estamos en modo Texto
                if (isDrawMode) return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startClickTime = System.currentTimeMillis();

                        // Seleccionar esta caja
                        setActiveText((EditText) view);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Si fue un click rápido, permitir editar
                        if (System.currentTimeMillis() - startClickTime < 200) {
                            view.performClick();
                            view.requestFocus();
                            // Mostrar teclado aquí si es necesario
                            return false;
                        }
                        return true;
                }
                return false;
            }
        });

        // Detectar foco para actualizar controles
        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) setActiveText((EditText) v);
        });

        textContainer.addView(et);
        setActiveText(et);
    }

    private void setActiveText(EditText et) {
        this.activeEditText = et;
        // Aplicar color seleccionado actual al nuevo texto
        et.setTextColor(currentColor);
    }
}