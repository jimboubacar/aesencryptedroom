/*
 CLASS: Note
 PURPOSE:
 Room entity representing a note with a non-sensitive title and a sensitive secret.
 The secret is stored in the database as AES-GCM ciphertext via Room TypeConverters.

 HOW TO USE:
 Create with: new Note("My title", EncryptedString.fromPlain("super secret"));
 Insert via:   db.noteDao().insert(note);
 When you read a Note back from Room, the 'secret' field is already decrypted
 (thanks to CryptoConverters) — use secret.getPlaintext() to access it.

 NOTES:
 Only the 'secret' column is encrypted at rest. 'title' is stored as plain text.
 The actual DB column name for the secret is 'secret_text'.
 */

package com.example.aesencryptedroom;                 // App package

import androidx.room.ColumnInfo;                       // Room column annotation
import androidx.room.Entity;                           // Marks this class as a Room table
import androidx.room.PrimaryKey;                       // Primary key annotation

@Entity(tableName = "notes")                           // Declare a Room table called "notes"
public class Note {

    @PrimaryKey(autoGenerate = true)                   // Auto-incrementing primary key
    public long id;                                    // Row ID

    public String title;                               // Non-sensitive title (stored as plain text)

    // Stored as ciphertext in DB via CryptoConverters
    @ColumnInfo(name = "secret_text")                  // Explicit DB column name for the secret
    public EncryptedString secret;                     // Sensitive field; encrypted/decrypted by converters

    public Note(String title, EncryptedString secret) { // Constructor for convenient creation
        this.title = title;                             // Assign title
        this.secret = secret;                           // Assign secret (wrap plaintext before passing)
    }
}
