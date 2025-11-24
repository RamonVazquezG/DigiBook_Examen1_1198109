package com.example.digibook_examen1_1198109;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<String> notesList;
    private OnNoteInteractionListener listener;

    // Interfaz actualizada para manejar clic y borrado
    public interface OnNoteInteractionListener {
        void onNoteClick(String noteName);
        void onNoteDelete(String noteName);
    }

    public NotesAdapter(List<String> notesList, OnNoteInteractionListener listener) {
        this.notesList = notesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        String noteName = notesList.get(position);
        holder.bind(noteName, listener);
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public void updateData(List<String> newNotes) {
        this.notesList = newNotes;
        notifyDataSetChanged();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        View containerNoteData;
        ImageButton btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textNoteName);
            containerNoteData = itemView.findViewById(R.id.containerNoteData);
            btnDelete = itemView.findViewById(R.id.btnDeleteNote);
        }

        public void bind(final String name, final OnNoteInteractionListener listener) {
            textName.setText(name);

            // Clic en la tarjeta para abrir (usamos itemView para toda la tarjeta excepto el botón borrar)
            itemView.setOnClickListener(v -> listener.onNoteClick(name));

            // Clic en el botón borrar
            btnDelete.setOnClickListener(v -> listener.onNoteDelete(name));
        }
    }
}