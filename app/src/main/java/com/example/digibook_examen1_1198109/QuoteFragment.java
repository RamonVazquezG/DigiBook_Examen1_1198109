package com.example.digibook_examen1_1198109;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.navigation.Navigation;

import com.example.digibook_examen1_1198109.databinding.FragmentQuoteBinding;

public class QuoteFragment extends Fragment implements LoaderManager.LoaderCallbacks<String> {

    // Binding
    private FragmentQuoteBinding binding;
    private static final int QUOTE_LOADER_ID = 1;

    // Infla el layout
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuoteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    // Configura Loader y botones
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Iniciar carga
        LoaderManager.getInstance(this).initLoader(QUOTE_LOADER_ID, null, this);

        binding.buttonCloseQuote.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );
    }

    // Crear Loader
    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
        return new QuoteLoader(requireContext());
    }

    // Manejar datos cargados
    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        binding.progressBarQuote.setVisibility(View.GONE);
        if (data != null) {
            binding.textQuoteContent.setText(data);
        } else {
            binding.textQuoteContent.setText("No se pudo obtener la cita. Revisa tu conexión.");
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {
        // No necesario
    }

    // Limpiar binding
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}