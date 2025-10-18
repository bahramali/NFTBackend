package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import se.hydroleaf.model.Note;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(NoteService.class)
class NoteServiceTest {

    @Autowired
    private NoteService noteService;

    @Test
    void updateExistingNote() {
        Note saved = noteService.save(Note.builder()
                .title("Old")
                .date(LocalDateTime.now())
                .content("Initial")
                .build());

        Note updated = Note.builder()
                .title("New")
                .date(LocalDateTime.now())
                .content("Updated")
                .build();

        Note result = noteService.update(saved.getId(), updated);
        assertEquals("New", result.getTitle());
        assertEquals("Updated", result.getContent());
    }

    @Test
    void saveSetsDateWhenNull() {
        Note saved = noteService.save(Note.builder()
                .title("With date")
                .content("No date provided")
                .build());

        assertNotNull(saved.getDate());
    }

    @Test
    void updateIgnoresNullDate() {
        LocalDateTime originalDate = LocalDateTime.now();
        Note saved = noteService.save(Note.builder()
                .title("Old")
                .date(originalDate)
                .content("Initial")
                .build());

        Note updated = Note.builder()
                .title("New")
                .content("Updated")
                .build();

        Note result = noteService.update(saved.getId(), updated);
        assertEquals(originalDate, result.getDate());
        assertEquals("New", result.getTitle());
    }

    @Test
    void updateNonExistingNoteThrowsException() {
        Note updated = Note.builder()
                .title("New")
                .date(LocalDateTime.now())
                .content("Updated")
                .build();

        assertThrows(IllegalArgumentException.class, () -> noteService.update(999L, updated));
    }
}
