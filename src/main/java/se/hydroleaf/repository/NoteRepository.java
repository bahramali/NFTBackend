package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.Note;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByContentContainingIgnoreCase(String query);
}
