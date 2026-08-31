package com.khush.contineologin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

/**
 * Loads the real Contineo parent portal in a WebView and autofills
 * YOUR saved USN / DOB / parent-verification digits into whatever
 * login form is currently on screen. All actual login logic still
 * happens on msrit's own servers — this app only fills text boxes
 * for you, it never talks to the site directly itself.
 */
class MainActivity : AppCompatActivity() {

    private val portalUrl = "https://parents.msrit.edu/newparents/index.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SettingsActivity.getPrefs(this)
        val usn = prefs.getString(SettingsActivity.KEY_USN, null)

        // First run -> go set up credentials
        if (usn.isNullOrEmpty()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val webView = findViewById<WebView>(R.id.webView)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnRefill = findViewById<Button>(R.id.btnRefill)
        val btnEdit = findViewById<Button>(R.id.btnEdit)

        setupWebView(webView, progressBar)
        webView.loadUrl(portalUrl)

        btnRefill.setOnClickListener {
            webView.evaluateJavascript(buildAutofillScript(), null)
        }

        btnEdit.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView, progressBar: ProgressBar) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = ProgressBar.GONE
                // Try autofilling automatically every time a page finishes loading.
                // Harmless if the fields it's looking for aren't on this particular page.
                view?.evaluateJavascript(buildAutofillScript(), null)
            }
        }

        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) ProgressBar.VISIBLE else ProgressBar.GONE
            }
        }
    }

    /**
     * Builds a JS snippet that tries several common selector patterns to find
     * the USN field, the DOB day/month/year dropdowns, the mother/father
     * radio choice, and the last-4-digits field -- then fills them with your
     * saved values. It does NOT click submit for you; you press the site's
     * own login/verify button yourself after checking the fields look right.
     *
     * NOTE: msrit's exact field names weren't reachable to inspect in advance
     * (the verification step appears to load dynamically). If auto-fill
     * misses a field, use Chrome's "Inspect" (chrome://inspect) with the
     * phone plugged into a laptop to find the real name/id and update the
     * selector lists below.
     */
    private fun buildAutofillScript(): String {
        val prefs = SettingsActivity.getPrefs(this)
        val usn = prefs.getString(SettingsActivity.KEY_USN, "") ?: ""
        val dob = prefs.getString(SettingsActivity.KEY_DOB, "") ?: "" // DD-MM-YYYY
        val parentType = prefs.getString(SettingsActivity.KEY_PARENT, "mother") ?: "mother"
        val last4 = prefs.getString(SettingsActivity.KEY_LAST4, "") ?: ""

        val parts = dob.split("-")
        val day = parts.getOrNull(0)?.trimStart('0')?.ifEmpty { "0" } ?: ""
        val monthNum = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val year = parts.getOrNull(2) ?: ""
        val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val monthAbbrev = if (monthNum in 1..12) monthNames[monthNum - 1] else ""

        return """
        (function() {
          function setSelectByText(select, text) {
            for (var i = 0; i < select.options.length; i++) {
              var optText = select.options[i].text.trim();
              if (optText === text || optText === text.toString() || optText.indexOf(text) === 0) {
                select.selectedIndex = i;
                select.dispatchEvent(new Event('change', { bubbles: true }));
                return true;
              }
            }
            return false;
          }
          function fillText(el, value) {
            if (!el || !value) return;
            el.value = value;
            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
          }

          // --- USN / username field ---
          var usnCandidates = document.querySelectorAll(
            'input[name*="user" i], input[id*="user" i], input[name*="usn" i], input[id*="usn" i]'
          );
          if (usnCandidates.length > 0) fillText(usnCandidates[0], "$usn");

          // --- DOB day/month/year selects ---
          var selects = document.querySelectorAll('select');
          for (var s = 0; s < selects.length; s++) {
            var el = selects[s];
            var hay = ((el.name || '') + ' ' + (el.id || '')).toLowerCase();
            if (hay.indexOf('day') !== -1) setSelectByText(el, "$day");
            else if (hay.indexOf('month') !== -1) setSelectByText(el, "$monthAbbrev");
            else if (hay.indexOf('year') !== -1) setSelectByText(el, "$year");
          }

          // --- Mother / Father radio choice ---
          var radios = document.querySelectorAll('input[type="radio"]');
          for (var r = 0; r < radios.length; r++) {
            var rd = radios[r];
            var hay2 = ((rd.value || '') + ' ' + (rd.id || '') + ' ' + (rd.name || '')).toLowerCase();
            var label = '';
            if (rd.id) {
              var lab = document.querySelector('label[for="' + rd.id + '"]');
              if (lab) label = lab.textContent.toLowerCase();
            }
            if ((hay2.indexOf("$parentType") !== -1) || (label.indexOf("$parentType") !== -1)) {
              rd.checked = true;
              rd.dispatchEvent(new Event('change', { bubbles: true }));
            }
          }

          // --- Last 4 digits field ---
          var last4Candidates = document.querySelectorAll(
            'input[name*="last" i], input[id*="last" i], input[name*="digit" i], input[id*="digit" i], input[name*="mobile" i], input[id*="mobile" i], input[maxlength="4"]'
          );
          if (last4Candidates.length > 0) fillText(last4Candidates[0], "$last4");
        })();
        """.trimIndent()
    }
}
