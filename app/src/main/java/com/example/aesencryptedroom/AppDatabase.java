/*
 CLASS: AppDatabase
 PURPOSE:
 Defines the Room database for this app, including entities and type converters.
 Exposes a singleton instance so you can access DAOs anywhere via AppDatabase.getInstance(context).

 HOW TO USE:
 1) Call AppDatabase.getInstance(context) to obtain the singleton database.
 2) From it, access your DAO(s), e.g. db.noteDao().
 3) Perform queries/inserts on a background thread; update UI on the main thread.

 NOTES:
  @TypeConverters registers CryptoConverters so the EncryptedString field is transparently
 encrypted on write and decrypted on read.
 The singleton uses double-checked locking with a volatile field for thread safety.
 .fallbackToDestructiveMigration() is fine for demos; replace with real migrations for production.
 */

package com.example.aesencryptedroom;                               // App package

import android.content.Context;                                      // For obtaining applicationContext

import androidx.room.Database;                                       // Room @Database annotation
import androidx.room.Room;                                           // Room.databaseBuilder(...)
import androidx.room.RoomDatabase;                                   // Base class for Room databases
import androidx.room.TypeConverters;                                 // Registers custom converters

@Database(entities = {Note.class},                                   // Register entity tables (here: Note)
        version = 1,                                               // Schema version for migrations
        exportSchema = false)                                      // Disable schema export (demo-friendly)
@TypeConverters({CryptoConverters.class})                            // Use CryptoConverters for EncryptedString
public abstract class AppDatabase extends RoomDatabase {             // Concrete Room DB definition
    public abstract NoteDao noteDao();                               // Expose DAO accessor

    private static volatile AppDatabase INSTANCE;                     // Singleton reference (volatile for DCL)

    public static AppDatabase getInstance(Context context) {          // Global accessor for the singleton DB
        if (INSTANCE == null) {                                       // Fast path: if already created, return it
            synchronized (AppDatabase.class) {                        // Lock to ensure single initialisation
                if (INSTANCE == null) {                               // Double-check in case another thread won
                    INSTANCE = Room.databaseBuilder(                  // Build a Room DB instance
                                    context.getApplicationContext(),  // Use app context to avoid leaks
                                    AppDatabase.class,                // Database class
                                    "encrypted-notes.db")             // On-disk filename
                            .fallbackToDestructiveMigration()         // Wipes DB on version change (demo only)
                            .build();                                 // Create the database
                }
            }
        }
        return INSTANCE;                                              // Return the singleton
    }
}
