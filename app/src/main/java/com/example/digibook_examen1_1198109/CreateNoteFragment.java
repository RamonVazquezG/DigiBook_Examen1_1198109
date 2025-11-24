package com.example.digibook_examen1_1198109;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import com.example.digibook_examen1_1198109.R;

import java.util.ArrayList;
import java.util.List;

public class CreateNoteFragment extends Fragment {

    private DrawingView drawingView;
    private FrameLayout textContainer;
    private FrameLayout workArea;
    private LinearLayout brushControls, textControls, colorPalette;
    private MaterialButton btnModeDraw, btnModeText, btnModePan, btnModeEraser, btnAddText;

    private EditText activeEditText;

    // Modos de operación
    private static final int MODE_DRAW = 0;
    private static final int MODE_TEXT = 1;
    private static final int MODE_PAN = 2;   // Mano (Mover items o la hoja)
    private static final int MODE_ERASER = 3; // Borrador
    private int currentMode = MODE_DRAW;

    private int currentColor = Color.BLACK;
    private List<View> colorViews = new ArrayList<>();

    // Zoom y Navegación Global
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 5.0f;

    // Variables para lógica de toque (Pan/Drag)
    private float lastTouchX, lastTouchY;
    private float globalPosX = 0f, globalPosY = 0f;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    // Objeto que se está arrastrando actualmente (Trazo de dibujo)
    private DrawingView.Stroke draggingStroke = null;
    private boolean isDraggingItem = false; // True si estamos moviendo un item (texto o trazo), False si movemos la hoja

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

        initViews(view);
        setupTouchLogic();
        setupControls();
        createColorButtons();

        setMode(MODE_DRAW);
    }

    private void initViews(View view) {
        workArea = view.findViewById(R.id.workArea);
        drawingView = view.findViewById(R.id.drawingView);
        textContainer = view.findViewById(R.id.textContainer);

        brushControls = view.findViewById(R.id.brushControls);
        textControls = view.findViewById(R.id.textControls);
        colorPalette = view.findViewById(R.id.colorPalette);

        btnModeDraw = view.findViewById(R.id.btnModeDraw);
        btnModeText = view.findViewById(R.id.btnModeText);
        btnModePan = view.findViewById(R.id.btnModePan);
        btnModeEraser = view.findViewById(R.id.btnModeEraser); // Nuevo botón
        btnAddText = view.findViewById(R.id.btnAddText);
    }

    private void setupTouchLogic() {
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleListener());

        // El Listener principal en drawingView coordina Dibujo, Selección y Movimiento Global
        drawingView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);

            // Obtener coordenadas relativas a la vista
            float x = event.getX();
            float y = event.getY();

            // Coordenadas raw para cálculos de delta precisos en pantalla
            float rawX = event.getRawX();
            float rawY = event.getRawY();

            final int action = event.getActionMasked();

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    activePointerId = event.getPointerId(0);
                    lastTouchX = rawX;
                    lastTouchY = rawY;
                    isDraggingItem = false;

                    // 1. LÓGICA MODO BORRADOR
                    if (currentMode == MODE_ERASER) {
                        DrawingView.Stroke strokeToDelete = drawingView.findStrokeAt(x, y);
                        if (strokeToDelete != null) {
                            drawingView.deleteStroke(strokeToDelete);
                            return true;
                        }
                        // Si no tocó trazo, podría haber tocado texto, pero eso lo maneja el OnTouch del texto
                    }

                    // 2. LÓGICA MODO MANO (Mover Items)
                    if (currentMode == MODE_PAN) {
                        // Intentar agarrar un trazo
                        draggingStroke = drawingView.findStrokeAt(x, y);
                        if (draggingStroke != null) {
                            isDraggingItem = true;
                        }
                        // Si no agarramos trazo, moveremos la hoja (lógica abajo en ACTION_MOVE)
                    }

                    // 3. LÓGICA MODO DIBUJO
                    if (currentMode == MODE_DRAW && event.getPointerCount() == 1) {
                        drawingView.startPath(x, y);
                        return true; // Consumimos para dibujar
                    }
                    break;
                }

                case MotionEvent.ACTION_MOVE: {
                    if (activePointerId == MotionEvent.INVALID_POINTER_ID) break;

                    float dx = rawX - lastTouchX;
                    float dy = rawY - lastTouchY;

                    // A. Si estamos dibujando
                    if (currentMode == MODE_DRAW && !scaleGestureDetector.isInProgress() && event.getPointerCount() == 1) {
                        drawingView.movePath(x, y);
                        lastTouchX = rawX;
                        lastTouchY = rawY;
                        return true;
                    }

                    // B. Si estamos moviendo un ITEM (Trazo)
                    if (currentMode == MODE_PAN && isDraggingItem && draggingStroke != null) {
                        // Ajustamos dx/dy por el factor de escala inverso para que el movimiento siga al dedo 1:1 visualmente
                        drawingView.moveStroke(draggingStroke, dx / scaleFactor, dy / scaleFactor);
                        lastTouchX = rawX;
                        lastTouchY = rawY;
                        return true;
                    }

                    // C. Movimiento Global de la Hoja (Pan)
                    // Ocurre si es modo PAN y no agarramos item, O si usamos 2 dedos en cualquier modo
                    if ((currentMode == MODE_PAN && !isDraggingItem) || scaleGestureDetector.isInProgress() || event.getPointerCount() > 1) {
                        globalPosX += dx;
                        globalPosY += dy;
                        applyTransformations();
                        lastTouchX = rawX;
                        lastTouchY = rawY;
                        return true;
                    }
                    break;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (currentMode == MODE_DRAW && !isDraggingItem && event.getPointerCount() == 1) {
                        drawingView.endPath();
                    }

                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    draggingStroke = null;
                    isDraggingItem = false;
                    break;
                }
            }

            // Si es modo Borrador o Mano, siempre consumimos para evitar comportamientos raros
            if (currentMode == MODE_ERASER || currentMode == MODE_PAN) return true;

            return false;
        });
    }

    private void setupControls() {
        btnModeDraw.setOnClickListener(v -> setMode(MODE_DRAW));
        btnModeText.setOnClickListener(v -> setMode(MODE_TEXT));
        btnModePan.setOnClickListener(v -> setMode(MODE_PAN));
        btnModeEraser.setOnClickListener(v -> setMode(MODE_ERASER));
        btnAddText.setOnClickListener(v -> addDraggableText());

        Slider sliderBrush = getView().findViewById(R.id.sliderBrushSize);
        Slider sliderText = getView().findViewById(R.id.sliderTextSize);

        sliderBrush.addOnChangeListener((slider, value, fromUser) -> drawingView.setBrushSize((int) value));
        sliderText.addOnChangeListener((slider, value, fromUser) -> {
            if (activeEditText != null) activeEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
        });
    }

    // --- Lógica de Transformación Global ---
    private void applyTransformations() {
        workArea.setScaleX(scaleFactor);
        workArea.setScaleY(scaleFactor);
        workArea.setTranslationX(globalPosX);
        workArea.setTranslationY(globalPosY);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
            applyTransformations();
            return true;
        }
    }

    // --- Gestión de Modos ---
    private void setMode(int mode) {
        currentMode = mode;

        drawingView.setDrawingEnabled(mode == MODE_DRAW);

        brushControls.setVisibility(mode == MODE_DRAW ? View.VISIBLE : View.GONE);
        textControls.setVisibility(mode == MODE_TEXT ? View.VISIBLE : View.GONE);
        btnAddText.setVisibility(mode == MODE_TEXT ? View.VISIBLE : View.GONE);

        updateButtonStyle(btnModeDraw, mode == MODE_DRAW);
        updateButtonStyle(btnModeText, mode == MODE_TEXT);
        updateButtonStyle(btnModePan, mode == MODE_PAN);
        updateButtonStyle(btnModeEraser, mode == MODE_ERASER);

        if (mode != MODE_TEXT && activeEditText != null) {
            activeEditText.setBackgroundResource(0);
            activeEditText.clearFocus();
            activeEditText = null;
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

    // --- Lógica de Texto ---
    private void addDraggableText() {
        EditText et = new EditText(requireContext());
        et.setText("Texto");
        et.setTextColor(currentColor);
        et.setTextSize(24);
        et.setBackgroundResource(0);
        et.setPadding(16, 16, 16, 16);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        et.setLayoutParams(params);

        // Touch Listener específico para el Texto
        et.setOnTouchListener((view, event) -> {
            // 1. SI ES BORRADOR: Borrar texto al tocar
            if (currentMode == MODE_ERASER) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    textContainer.removeView(view);
                    if (activeEditText == view) activeEditText = null;
                }
                return true;
            }

            // 2. SI ES DIBUJO: Ignorar texto (para no moverlo por error)
            if (currentMode == MODE_DRAW) return false;

            // 3. SI ES MANO O TEXTO: Permitir mover
            // NOTA: En modo TEXTO también permitimos mover para acomodarlo
            if (currentMode == MODE_PAN || currentMode == MODE_TEXT) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        if (currentMode == MODE_TEXT) setActiveText((EditText) view);
                        return true; // Consumir evento para empezar drag

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - lastTouchX;
                        float dy = event.getRawY() - lastTouchY;

                        // Mover la vista ajustando por la escala para que siga al dedo
                        view.setX(view.getX() + (dx / scaleFactor));
                        view.setY(view.getY() + (dy / scaleFactor));

                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Si fue un clic rápido en modo TEXTO, dar foco para editar
                        if (currentMode == MODE_TEXT) {
                            view.performClick();
                            view.requestFocus();
                            return false; // Dejar pasar para que salga el teclado
                        }
                        return true;
                }
            }
            return false;
        });

        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentMode == MODE_TEXT) {
                setActiveText((EditText) v);
            } else {
                v.setBackgroundResource(0);
            }
        });

        textContainer.addView(et);
        setActiveText(et);
    }

    private void setActiveText(EditText et) {
        if (activeEditText != null && activeEditText != et) {
            activeEditText.setBackgroundResource(0);
        }
        this.activeEditText = et;
        if (currentMode == MODE_TEXT) {
            et.setBackgroundResource(R.drawable.bg_text_selection);
        }
    }

    private void createColorButtons() {
        colorPalette.removeAllViews();
        colorViews.clear();
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        for (String colorHex : colors) {
            int parsedColor = Color.parseColor(colorHex);
            View colorView = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            colorView.setLayoutParams(params);
            colorView.setTag(parsedColor);
            updateColorViewBackground(colorView, parsedColor, parsedColor == currentColor);

            colorView.setOnClickListener(v -> {
                int selectedColor = (int) v.getTag();
                currentColor = selectedColor;
                drawingView.setColor(String.format("#%06X", (0xFFFFFF & selectedColor)));
                if (activeEditText != null) activeEditText.setTextColor(currentColor);
                refreshColorSelection();
            });
            colorViews.add(colorView);
            colorPalette.addView(colorView);
        }
    }

    private void refreshColorSelection() {
        for (View view : colorViews) {
            int color = (int) view.getTag();
            updateColorViewBackground(view, color, color == currentColor);
        }
    }

    private void updateColorViewBackground(View view, int color, boolean isSelected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        if (isSelected) {
            int borderColor = (color == Color.WHITE) ? Color.BLACK : Color.DKGRAY;
            shape.setStroke(8, borderColor);
        } else {
            shape.setStroke(2, Color.LTGRAY);
        }
        view.setBackground(shape);
    }
}