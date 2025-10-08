/*
CLASS: MainActivity
 PURPOSE:
 Provides a simple UI to demonstrate AES-GCM encryption/decryption with a Room database.
 Lets you type plaintext, encrypt it to ciphertext, decrypt it back, and save/load notes.

 HOW TO USE IN THE APP:
 1) Type a short title in the Title box.
 2) Type any text in the plaintext box.
 3) Tap "Encrypt" to see ciphertext appear in the non-editable field.
 4) Tap "Decrypt" to show the decrypted (plain) text back in the same non-editable field.
 5) Tap "Save Encrypted (to Room)" to store the note (secret is encrypted at rest).
 6) Tap "Load Last (from Room)" to fetch the most recent note (shows both plaintext and raw DB ciphertext).

 NOTES:
 All crypto uses Android Keystore with provider-generated IVs (AES/GCM/NoPadding).
 Room TypeConverters transparently encrypt on write and decrypt on read.
 Long-running DB work runs off the main thread; UI updates run on the main thread.
 */

package com.example.aesencryptedroom;                              // App package name

import android.os.Bundle;                                           // Android lifecycle bundle
import android.text.TextUtils;                                      // Utility for empty/string checks
import android.view.View;
import android.widget.Button;                                       // UI button widget
import android.widget.EditText;                                     // UI editable text field
import android.widget.TextView;                                     // UI non-editable text field
import android.widget.Toast;                                        // Short user messages

import androidx.appcompat.app.AppCompatActivity;                    // Base activity with AppCompat support

import java.util.concurrent.Executors;                              // Background threading utilities

public class MainActivity extends AppCompatActivity {               // Main entry activity for the demo screen
    private static final String TAG = "MainActivity";               // Tag for Logcat

    private AppDatabase db;                                         // Singleton Room database
    private EditText etTitle, etPlain;                              // Title and plaintext input fields
    private TextView tvCipher, tvResult;                            // Non-editable display (cipher/plain) and status/result

    @Override
    protected void onCreate(Bundle savedInstanceState) {            // Activity lifecycle: creation
        super.onCreate(savedInstanceState);                         // Call base implementation
        setContentView(R.layout.activity_main);                     // Inflate UI layout

        db = AppDatabase.getInstance(this);                         // Get Room DB singleton

        etTitle  = findViewById(R.id.etTitle);                      // Bind title input
        etPlain  = findViewById(R.id.etPlain);                      // Bind plaintext input
        tvCipher = findViewById(R.id.tvCipher);                     // Bind ciphertext/decrypted display
        tvResult = findViewById(R.id.tvResult);                     // Bind result/status text

        Button btnEncrypt  = findViewById(R.id.btnEncrypt);         // Bind Encrypt button
        Button btnDecrypt  = findViewById(R.id.btnDecrypt);         // Bind Decrypt button
        Button btnSave     = findViewById(R.id.btnSave);            // Bind Save button
        Button btnLoadLast = findViewById(R.id.btnLoadLast);        // Bind Load Last button

        // Start with Decrypt disabled until we have ciphertext
        btnDecrypt.setEnabled(false);                               // Avoid decrypting when no CT exists

        btnEncrypt.setOnClickListener(v -> doEncrypt(btnDecrypt));  // Encrypt handler (enables decrypt)
        btnDecrypt.setOnClickListener(v -> doDecrypt());            // Decrypt handler (shows plain in tvCipher)
        btnSave.setOnClickListener(v -> saveNote());                // Save encrypted note to Room

        //old way of doing things
        btnLoadLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLast();
            }
        });
        //same as below


        //btnLoadLast.setOnClickListener(v -> loadLast());            // Load last note from Room
    }

    private void doEncrypt(Button btnDecrypt) {                     // Encrypt the text currently in etPlain
        String plain = etPlain.getText().toString();                // Read plaintext from input
        if (TextUtils.isEmpty(plain)) {                             // Guard: must have something to encrypt
            Toast.makeText(this, "Please type some plaintext first.", Toast.LENGTH_SHORT).show(); // Inform user
            return;                                                 // Abort if empty
        }
        try {
            String ct = CryptoUtil.encryptToBase64IvCt(plain);      // Encrypt with AES-GCM (returns Base64IV:Base64CT)
            tvCipher.setText(ct);                                   // Show ciphertext in non-editable field
            btnDecrypt.setEnabled(true);                            // Enable decrypt (now we have CT)
            Toast.makeText(this, "Encrypted.", Toast.LENGTH_SHORT).show(); // Feedback
        } catch (Exception e) {                                     // Handle any crypto error
            Toast.makeText(this, "Encrypt failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); // Show reason
        }
    }

    private void doDecrypt() {                                      // Decrypt whatever is in the non-editable field
        String raw = tvCipher.getText().toString();                 // Read current content (expected IV:CT)
        String sanitized = raw.replaceAll("\\s", "");               // Remove spaces/newlines/tabs (pasted text safety)

        // Expect Base64IV:Base64CT
        int colon = sanitized.indexOf(':');                         // Find separator between IV and CT
        if (TextUtils.isEmpty(sanitized) ||                         // Guard: must not be empty
                colon <= 0 || colon >= sanitized.length() - 1) {        // Guard: colon must be present and not at ends
            Toast.makeText(this, "No valid ciphertext (expected Base64IV:Base64CT).", Toast.LENGTH_SHORT).show(); // Inform
            return;                                                 // Abort if malformed
        }

        try {
            String pt = CryptoUtil.decryptFromBase64IvCt(sanitized);// Decrypt using IV and CT (AES-GCM)
            tvCipher.setText(pt);                                   // Show ONLY decrypted plaintext in non-editable field
            // etPlain.setText(pt);                                  // (Optional) also mirror into editable box
            Toast.makeText(this, "Decrypted.", Toast.LENGTH_SHORT).show(); // Feedback
        } catch (Exception e) {                                     // Handle auth/tag/keystore errors
            Toast.makeText(this, "Decrypt failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); // Show reason
        }
    }

    // === Room demo (unchanged conceptually; now saving the current plaintext) ===
    private void saveNote() {                                       // Persist a note with encrypted secret
        String title = etTitle.getText().toString().trim();         // Read title
        String plain = etPlain.getText().toString();                // Read plaintext (to be encrypted by converter)

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(plain)) { // Guard: require both fields
            Toast.makeText(this, "Please enter both title and plaintext.", Toast.LENGTH_SHORT).show(); // Inform user
            return;                                                 // Abort if missing input
        }

        Executors.newSingleThreadExecutor().execute(() -> {         // Do DB work off the main thread
            try {
                // Room TypeConverter will encrypt this at write time
                Note note = new Note(title, EncryptedString.fromPlain(plain)); // Build entity (secret as EncryptedString)
                long id = db.noteDao().insert(note);                // Insert and get new row id

                // Read back raw ciphertext from DB for display
                String ciphertext = db.noteDao().getCiphertextById(id); // Fetch stored Base64IV:Base64CT from column

                String out = "Saved Note\n" +                       // Human-readable status text
                        "ID: " + id + "\n" +
                        "Title: " + title + "\n\n" +
                        "Decrypted (in app):\n" + plain + "\n\n" +
                        "Stored ciphertext (in DB):\n" + ciphertext;

                runOnUiThread(() -> {                               // Post UI updates back to main thread
                    tvCipher.setText(ciphertext);                   // Show ciphertext in the display field
                    tvResult.setText(out);                          // Show the status/details
                    Toast.makeText(MainActivity.this, "Saved (encrypted in DB).", Toast.LENGTH_SHORT).show(); // Feedback
                });
            } catch (Exception e) {                                 // Handle DB/crypto exceptions
                android.util.Log.e(TAG, "Save note failed", e);     // Log full stacktrace for debugging
                runOnUiThread(() ->                                 // Inform the user without leaking details
                        Toast.makeText(MainActivity.this, "Save failed. Please try again.", Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void loadLast() {                                       // Load the most recently saved note
        Executors.newSingleThreadExecutor().execute(() -> {         // Off the main thread
            try {
                Note last = db.noteDao().getLast();                 // Fetch last note entity
                if (last == null) {                                 // If DB empty
                    runOnUiThread(() -> tvResult.setText(R.string.no_notes_saved_yet)); // Show “no notes” message
                    return;                                         // Nothing else to do
                }
                String decrypted = last.secret != null              // Room has already decrypted secret via converter
                        ? last.secret.getPlaintext() : null;        // Get plaintext from wrapper
                String ciphertext = db.noteDao().getCiphertextById(last.id); // Fetch raw stored ciphertext

                String out = "Last Note\n" +                        // Prepare details text
                        "ID: " + last.id + "\n" +
                        "Title: " + last.title + "\n\n" +
                        "Decrypted secret (in app):\n" + decrypted + "\n\n" +
                        "Stored ciphertext (in DB):\n" + ciphertext;

                runOnUiThread(() -> {                               // Update UI on main thread
                    etPlain.setText(decrypted);                     // Put plaintext into editable box
                    tvCipher.setText(ciphertext);                   // Show ciphertext in display field
                    tvResult.setText(out);                          // Show details/status
                });
            }  catch (Exception e) {                                // Handle DB/crypto exceptions
                android.util.Log.e(TAG, "Load note failed", e);     // Log stacktrace for debugging
                runOnUiThread(() ->                                 // Show generic failure toast
                        Toast.makeText(MainActivity.this, "Load failed. Please try again.", Toast.LENGTH_LONG).show()
                );
            }
        });
    }
}
