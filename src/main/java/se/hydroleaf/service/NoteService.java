package se.hydroleaf.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.hydroleaf.model.Note;
import se.hydroleaf.repository.NoteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public Note save(Note note) {
        return noteRepository.save(note);
    }

    public List<Note> searchByContent(String query) {
        return noteRepository.findByContentContainingIgnoreCase(query);
    }
}
