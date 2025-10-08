/*
 CLASS: CryptoConverters
 PURPOSE:
 Room TypeConverters that transparently encrypt and decrypt the Note.secret field.
 On write: plaintext (inside EncryptedString) -> AES-GCM ciphertext string "Base64(IV):Base64(CT)".
 On read: ciphertext string -> decrypted EncryptedString held in memory as plaintext.

 HOW TO USE:
 Register these converters on your Room database class with @TypeConverters({CryptoConverters.class}).
 In your entity, declare the sensitive column as EncryptedString (not String).
 From app code, work with plaintext via EncryptedString; Room will handle at-rest encryption.

 NOTES:
 CryptoUtil generates provider IVs on encrypt and validates GCM tags on decrypt.
 The database never stores plaintext; only the IV:CT Base64 string is persisted.
 */

package com.example.aesencryptedroom;                 // App package

import androidx.room.TypeConverter;                   // Room annotation for custom converters

/**
 * Converts between EncryptedString (plaintext in memory) and the DB column
 * that stores ciphertext as a Base64 "IV:CT" string.
 */
public class CryptoConverters {

    @TypeConverter
    public static String toCiphertext(EncryptedString value) { // Called by Room when writing the entity
        if (value == null) return null;                        // Null-safe: keep nulls as nulls
        return CryptoUtil.encryptToBase64IvCt(                 // Encrypt plaintext using AES-GCM
                value.getPlaintext());                         // Extract plaintext from wrapper
    }

    @TypeConverter
    public static EncryptedString toEncryptedString(String dbValue) { // Called by Room when reading the entity
        if (dbValue == null) return null;                              // Null-safe: keep nulls as nulls
        String plaintext = CryptoUtil.decryptFromBase64IvCt(dbValue);  // Decrypt IV:CT string back to plaintext
        return EncryptedString.fromPlain(plaintext);                   // Re-wrap plaintext for in-memory use
    }
}
