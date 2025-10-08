/*
 INTERFACE: NoteDao
 PURPOSE:
 Defines Room data-access methods for the Note entity.
 Supports inserting notes, fetching the latest note, reading raw ciphertext for a note,
 and listing all notes.

 HOW TO USE:
   - Obtain an instance from the database: NoteDao dao = AppDatabase.getInstance(ctx).noteDao();
   - Insert: long id = dao.insert(new Note("Title", EncryptedString.fromPlain("secret")));
   - Read last note (already decrypted via converters): Note n = dao.getLast();
   - Inspect at-rest encryption: String ct = dao.getCiphertextById(id); // Base64(IV):Base64(CT)
   - List all: List<Note> notes = dao.getAll();

NOTES:
 Room auto-generates the implementation at compile time.
 - Methods must be called off the main thread unless you configure Room for main-thread queries.
 */

package com.example.aesencryptedroom;             // App package

import androidx.room.Dao;                          // Marks this interface as a Room DAO
import androidx.room.Insert;                       // Annotation for INSERT operation
import androidx.room.Query;                        // Annotation for raw SQL queries

import java.util.List;                             // For returning lists of entities

@Dao                                              // Room will generate the backing implementation
public interface NoteDao {

    @Insert                                       // INSERT the given Note; returns new row ID
    long insert(Note note);

    @Query("SELECT * FROM notes ORDER BY id DESC LIMIT 1") // Fetch the most recently inserted note
    Note getLast();

    // Return the raw ciphertext string from DB to prove it is encrypted at rest.
    @Query("SELECT secret_text FROM notes WHERE id = :id LIMIT 1") // Read stored Base64(IV):Base64(CT)
    String getCiphertextById(long id);

    @Query("SELECT * FROM notes")                 // Fetch all notes (secrets auto-decrypted via converters)
    List<Note> getAll();
}
