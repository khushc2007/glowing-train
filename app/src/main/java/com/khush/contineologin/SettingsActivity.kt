package com.khush.contineologin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Lets you enter YOUR OWN Contineo login details once.
 * Everything is stored in EncryptedSharedPreferences, on-device only.
 * Nothing is ever sent anywhere except directly to parents.msrit.edu
 * when the WebView loads it into the real site's own login form.
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_FILE = "contineo_secure_prefs"
        const val KEY_USN = "usn"
        const val KEY_DOB = "dob" // stored as DD-MM-YYYY
        const val KEY_PARENT = "parent_type" // "mother" or "father"
        const val KEY_LAST4 = "last4"

        fun getPrefs(context: Context) = run {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etUsn = findViewById<EditText>(R.id.etUsn)
        val etDob = findViewById<EditText>(R.id.etDob)
        val etLast4 = findViewById<EditText>(R.id.etLast4)
        val rgParent = findViewById<RadioGroup>(R.id.rgParent)
        val btnSave = findViewById<android.widget.Button>(R.id.btnSave)

        val prefs = getPrefs(this)

        // Pre-fill if already saved before
        etUsn.setText(prefs.getString(KEY_USN, ""))
        etDob.setText(prefs.getString(KEY_DOB, ""))
        etLast4.setText(prefs.getString(KEY_LAST4, ""))
        when (prefs.getString(KEY_PARENT, "mother")) {
            "father" -> rgParent.check(R.id.rbFather)
            else -> rgParent.check(R.id.rbMother)
        }

        btnSave.setOnClickListener {
            val usn = etUsn.text.toString().trim().uppercase()
            val dob = etDob.text.toString().trim()
            val last4 = etLast4.text.toString().trim()
            val parentType = if (rgParent.checkedRadioButtonId == R.id.rbFather) "father" else "mother"

            if (usn.isEmpty() || dob.isEmpty() || last4.length != 4) {
                Toast.makeText(this, "Please fill USN, DOB and 4-digit code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString(KEY_USN, usn)
                .putString(KEY_DOB, dob)
                .putString(KEY_PARENT, parentType)
                .putString(KEY_LAST4, last4)
                .apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
