/*
 CLASS: EncryptedString
 PURPOSE:
 A lightweight wrapper used to mark sensitive text in entities so Room applies our TypeConverters.
 In memory, it carries the plaintext; in the database, Room (via CryptoConverters) stores only ciphertext.

 OW TO USE:
 In your Room entity, declare sensitive columns as EncryptedString (not String).
 e.g., public EncryptedString secret;
 When creating an entity, wrap plaintext with EncryptedString.fromPlain("your secret").
 When reading an entity back, call getPlaintext() to access the decrypted value.

 NOTES:
 The database never stores this plaintext; CryptoConverters encrypt/decrypt transparently.
 Keep this wrapper small and immutable to reduce accidental exposure and threading issues.
 */

package com.example.aesencryptedroom;          // App package

/**
 * Wrapper type to mark data as sensitive so Room will use our TypeConverter.
 * Internally holds plaintext only in memory; DB column stores ciphertext (Base64 IV:CT).
 */
public class EncryptedString {
    private final String plaintext;            // Immutable plaintext held only in app memory

    private EncryptedString(String plaintext) { // Private ctor enforces factory usage
        this.plaintext = plaintext;            // Store plaintext as-is (encryption happens in converters)
    }

    public static EncryptedString fromPlain(String plaintext) { // Factory: wrap a plaintext string
        return new EncryptedString(plaintext);                  // Return a new immutable wrapper
    }

    public String getPlaintext() {             // Accessor for the decrypted/plain value
        return plaintext;                      // Returns the raw plaintext (handle with care)
    }

    @Override
    public String toString() {                 // String representation (be mindful of logging!)
        return plaintext;                      // Returns plaintext; avoid logging this in production
    }
}
