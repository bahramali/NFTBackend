package se.hydroleaf.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.model.Note;
import se.hydroleaf.repository.NoteRepository;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoteRepository noteRepository;

    @BeforeEach
    void clearRepository() {
        noteRepository.deleteAll();
    }

    @Test
    void getNotesReturnsStoredNotes() throws Exception {
        noteRepository.save(Note.builder()
                .title("test title")
                .date(LocalDateTime.parse("2023-01-01T10:15:30"))
                .content("test note")
                .build());

        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("test title"))
                .andExpect(jsonPath("$[0].date").value("2023-01-01T10:15:30"))
                .andExpect(jsonPath("$[0].content").value("test note"));
    }

    @Test
    void postCreatesNote() throws Exception {
        String payload = "{\"title\":\"new note\",\"date\":\"2023-02-02T15:30:00\",\"content\":\"new content\"}";

        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("new note"))
                .andExpect(jsonPath("$.date").value("2023-02-02T15:30:00"))
                .andExpect(jsonPath("$.content").value("new content"));

        assertThat(noteRepository.findAll())
                .extracting(Note::getTitle, Note::getDate, Note::getContent)
                .containsExactly(tuple("new note", LocalDateTime.parse("2023-02-02T15:30:00"), "new content"));
    }

    @Test
    void searchReturnsNotesContainingQuery() throws Exception {
        noteRepository.save(Note.builder()
                .title("first")
                .date(LocalDateTime.parse("2023-03-03T12:00:00"))
                .content("some apple text")
                .build());
        noteRepository.save(Note.builder()
                .title("second")
                .date(LocalDateTime.parse("2023-04-04T12:00:00"))
                .content("banana content")
                .build());

        mockMvc.perform(get("/api/notes/search")
                        .param("query", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("some apple text"))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void putUpdatesNoteWithoutDate() throws Exception {
        Note saved = noteRepository.save(Note.builder()
                .title("before")
                .date(LocalDateTime.parse("2023-01-01T10:15:30"))
                .content("initial")
                .build());

        String payload = "{\\"title\\":\\"after\\",\\"content\\":\\"updated\\"}";

        mockMvc.perform(put("/api/notes/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("after"))
                .andExpect(jsonPath("$.date").value("2023-01-01T10:15:30"))
                .andExpect(jsonPath("$.content").value("updated"));
    }
}
