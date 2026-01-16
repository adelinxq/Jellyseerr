package com.jellyseerr.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.NotificationManager
import android.view.ViewOutlineProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: View
    private lateinit var rootLayout: FrameLayout
    private lateinit var logoImageView: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private val JELLYSEERR_URL = "https://jellyseerr-app.adelin.org/"

    private var isFirstLoad = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Găsește view-urile
        webView = findViewById(R.id.webView)
        splashView = findViewById(R.id.splashView)
        rootLayout = findViewById(R.id.rootLayout)

        // SETEAZĂ STATUS BAR ȘI NAVIGATION BAR MOV (#111827)
        val window: Window = window
        window.statusBarColor = Color.parseColor("#111827")
        window.navigationBarColor = Color.parseColor("#111827")

        // ASCUNDE SPLASH SCREEN-UL NATIV DE LA ANDROID
        hideNativeSplashScreen()

        // FORCE 120Hz
        enableHighRefreshRate()








        // Setup WebView
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // SETĂRI AVANSATE PENTRU PERFORMANȚĂ
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // PERMITE TOATE RESURSELE EXTERNE (pentru imagini CSS)
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.blockNetworkLoads = false
        settings.mediaPlaybackRequiresUserGesture = false

        // Pentru Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            settings.forceDark = WebSettings.FORCE_DARK_OFF
        }

        // USER AGENT CUSTOM
        val defaultUserAgent = settings.userAgentString
        settings.userAgentString = "$defaultUserAgent JellyseerrApp/1.0"

        // HARDWARE ACCELERATION
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Creează logo-ul FOLOSIND LOGO_FULL.PNG
        createLogoWithYourImage()
















// WebViewClient
        webView.webViewClient = object : WebViewClient() {

            // SSL ERROR HANDLING pentru domeniul tău
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: android.webkit.SslErrorHandler,
                error: android.net.http.SslError
            ) {
                if (error.url.contains("adelin.org") || error.url.contains("adelinx.go.ro")) {
                    handler.proceed()
                } else {
                    super.onReceivedSslError(view, handler, error)
                }
            }

            // DOAR O SINGURĂ FUNCȚIE onPageFinished!
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Elimină highlight-ul albastru
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(
                        """
                document.body.style.webkitTapHighlightColor = 'transparent';
                var allElements = document.getElementsByTagName('*');
                for(var i=0; i<allElements.length; i++) {
                    allElements[i].style.webkitTapHighlightColor = 'transparent';
                }
            """.trimIndent(), null
                    )
                }

                // ANIMAȚIE CROSSFADE DOAR LA PRIMA ÎNCĂRCARE
                // Verifică dacă URL-ul este homepage-ul și este prima încărcare
                if (isFirstLoad && url?.startsWith(JELLYSEERR_URL) == true) {
                    handler.postDelayed({
                        startCrossfadeAnimation()
                    }, 1)
                    isFirstLoad = false
                }
                // Dacă nu este prima încărcare, NU mai rulează animația
            }

            // ERROR HANDLING
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                if (errorCode == ERROR_HOST_LOOKUP || errorCode == ERROR_CONNECT) {
                    handler.post {
                        Toast.makeText(
                            this@MainActivity,
                            "Nu mă pot conecta la Jellyseerr. Verifică conexiunea la internet.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url ?: return false

                // DOAR acest URL EXACT rămâne în WebView
                if (url.startsWith("https://jellyseerr-app.adelin.org/") ||
                    url == "https://jellyseerr-app.adelin.org") {
                    return false // Rămâne în WebView
                }

                // Dacă este link relativ către home ("/")
                if (url == "/") {
                    // Navighează la homepage în WebView
                    view?.loadUrl(JELLYSEERR_URL)
                    return true // Spunem că am gestionat noi navigarea
                }

                // ORICE alt link care începe cu https:// sau http:// (inclusiv jellyseerr.adelin.org)
                if (url.startsWith("https://") || url.startsWith("http://")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return false
                    }
                }

                // Pentru linkurile interne (javascript:, mailto:, tel:, etc) - rămân în WebView
                return false
            }
        }











        // Adaugă WebChromeClient pentru permisiuni
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        // ARATĂ DIALOG-UL FRUMOS pentru notificări (după 1 secundă)
        handler.postDelayed({
            showBeautifulNotificationDialog()
        }, 1)

        // Începe cu ANIMAȚIA LOGO
        handler.postDelayed({
            startLogoPulseAnimation()
        }, 1)

        // Încarcă Jellyseerr
        handler.postDelayed({
            isFirstLoad = true  // Resetează pentru siguranță
            webView.loadUrl(JELLYSEERR_URL)
        }, 1)

        handler.postDelayed({
            checkForAppUpdate()
        }, 1000)
    }

    private fun showBeautifulNotificationDialog() {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val alreadyShown = sharedPreferences.getBoolean("notification_dialog_shown", false)

        // Verifică dacă notificările sunt deja activate
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Pentru Android < 13, notificările sunt by default activate
        }

        // Arată dialogul doar dacă:
        // 1. Nu a fost arătat deja
        // 2. Notificările nu sunt activate
        // 3. Este Android 13+ (pentru că la Android <13 notificările sunt by default ON)
        if (alreadyShown || notificationsEnabled) {
            return
        }

        // Creează dialogul frumos cu culori personalizate
        val dialog = AlertDialog.Builder(this)
            .setTitle("Bun venit la Jellyseerr!")
            .setMessage("Jellyseerr ar vrea să activeze notificările pentru cele mai noi noutăți despre Emby și Jellyseerr.")
            .setPositiveButton("Da, activeză") { dialog, _ ->
                // Salvează că a fost arătat
                sharedPreferences.edit().putBoolean("notification_dialog_shown", true).apply()

                dialog.dismiss()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permission = android.Manifest.permission.POST_NOTIFICATIONS
                    requestPermissions(arrayOf(permission), 123)
                } else {
                    Toast.makeText(this, "Notificările sunt acum activate! 🎉", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Nu, mulțumesc") { dialog, _ ->
                sharedPreferences.edit().putBoolean("notification_dialog_shown", true).apply()
                dialog.dismiss()
                Toast.makeText(this, "Poți activa notificările oricând din setări → aplicații → Jellyseerr", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(android.R.color.black)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#6D28D9"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9CA3AF"))

        try {
            val titleTextView = dialog.findViewById<android.widget.TextView>(android.R.id.title)
            val messageTextView = dialog.findViewById<android.widget.TextView>(android.R.id.message)

            titleTextView?.setTextColor(Color.WHITE)
            messageTextView?.setTextColor(Color.WHITE)
        } catch (e: Exception) {
            // Ignoră eroarea
        }
    }

    private fun hideNativeSplashScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                splashScreen?.setOnExitAnimationListener { splashScreenView ->
                    splashScreenView.remove()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        window.setBackgroundDrawableResource(android.R.color.transparent)
    }



















    private fun createLogoWithYourImage() {
        // CREEAZĂ UN GRADIENT MULT MAI FRUMOS PENTRU SPLASH SCREEN
        // Folosește un gradient diagonal cu mai multe culori pentru efect premium
        val premiumGradient = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR, // Diagonal din stânga-sus spre dreapta-jos
            intArrayOf(
                Color.parseColor("#0F172A"),    // Albastru închis premium
                Color.parseColor("#1E1B4B"),    // Violet intens
                Color.parseColor("#111827"),    // Culoarea ta principală (#111827)
                Color.parseColor("#1E293B")     // Albastru deschis
            )
        )

        // Setează corner radius pentru un efect mai modern
        premiumGradient.cornerRadius = 0f
        premiumGradient.gradientType = android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT

        // APLICĂ GRADIENTUL PREMIUM PE SPLASHVIEW
        splashView.background = premiumGradient

        // Creează logo-ul
        logoImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(200),
                dpToPx(200)
            ).apply {
                gravity = Gravity.CENTER
            }

            try {
                setImageResource(R.drawable.logo_full)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    setImageResource(R.mipmap.jellyseerricon)
                } catch (e2: Exception) {
                    setImageResource(android.R.drawable.ic_dialog_info)
                    setColorFilter(Color.parseColor("#6D28D9"))
                }
            }

            scaleType = ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = true
            alpha = 0f
            rotation = 0f
            elevation = 0f

            // Adaugă o umbră subtilă logo-ului pentru efect 3D
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dpToPx(8).toFloat()
                translationZ = dpToPx(8).toFloat()
            }
        }

        // ASIGURĂ-TE CĂ LOGO-UL E ADAUGAT ÎN LOCUL CORECT
        rootLayout.addView(logoImageView)
    }














    private fun startLogoPulseAnimation() {
        // Reset rotation la 0 pentru animație
        logoImageView.rotation = 0f

        logoImageView.animate()
            .alpha(1f)
            .setDuration(800)
            .withStartAction {
                logoImageView.visibility = View.VISIBLE
            }
            .start()

        val pulseAnimation = createPulseAnimation()
        logoImageView.startAnimation(pulseAnimation)
    }

    private fun createPulseAnimation(): Animation {
        val animationSet = AnimationSet(true)
        animationSet.interpolator = AccelerateDecelerateInterpolator()

        // SCALE ANIMATION simplă - fără rotație
        val scaleAnimation = ScaleAnimation(
            0.95f, 1.05f,  // SCALE X
            0.95f, 1.05f,  // SCALE Y
            Animation.RELATIVE_TO_SELF, 0.5f,  // PIVOT X CENTRU
            Animation.RELATIVE_TO_SELF, 0.5f   // PIVOT Y CENTRU
        )
        scaleAnimation.duration = 800
        scaleAnimation.repeatCount = Animation.INFINITE
        scaleAnimation.repeatMode = Animation.REVERSE

        // ALPHA ANIMATION simplă
        val alphaAnimation = AlphaAnimation(0.9f, 1.0f)
        alphaAnimation.duration = 1000
        alphaAnimation.repeatCount = Animation.INFINITE
        alphaAnimation.repeatMode = Animation.REVERSE

        // DOAR ACESTE DOUĂ ANIMAȚII, FĂRĂ ROTATE
        animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(alphaAnimation)

        return animationSet
    }











    private fun startCrossfadeAnimation() {
        // Stop pulse animation
        logoImageView.clearAnimation()

        // 1. Logo fades out with moderate zoom
        logoImageView.animate()
            .alpha(0f)
            .scaleX(2.4f)  // Natural zoom level
            .scaleY(2.4f)
            .setDuration(300)
            .withStartAction {
                // Make WebView visible but transparent
                webView.alpha = 0f
                webView.scaleX = 0.96f
                webView.scaleY = 0.96f
                webView.visibility = View.VISIBLE
            }
            .withEndAction {
                logoImageView.visibility = View.GONE
                // ASCUNDE LOGO-UL COMPLET după animație
                logoImageView.alpha = 0f

                // 2. WebView fades in with subtle zoom
                webView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(380)
                    .withEndAction {
                        // 3. Finally hide splash
                        splashView.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction {
                                splashView.visibility = View.GONE
                                // ASCUNDE SPLASH COMPLET
                                splashView.alpha = 0f
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }




    // === ADAUGĂ ACEASTĂ FUNCȚIE NOUĂ ===
    private fun removeBlueHighlight() {
        val jsCode = """
            // Elimină highlight-ul albastru de la toate elementele
            var style = document.createElement('style');
            style.innerHTML = '
                * {
                    -webkit-tap-highlight-color: transparent !important;
                    outline: none !important;
                }
                a, button, [role="button"], input, select, textarea {
                    -webkit-tap-highlight-color: rgba(0,0,0,0) !important;
                }
            ';
            document.head.appendChild(style);
        """.trimIndent()

        // Rulează codul JavaScript în WebView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript("(function(){$jsCode})()", null)
        }
    }
    // === SFÂRȘIT FUNCȚIE NOUĂ ===















    @SuppressLint("ObsoleteSdkInt")
    private fun enableHighRefreshRate() {
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        // Set high refresh rate for supported devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                window.attributes = window.attributes.apply {
                    @Suppress("DEPRECATION")
                    preferredDisplayModeId = 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // WebView optimizations
        webView.settings.apply {
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            setEnableSmoothTransition(true)
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        enableHighRefreshRate()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 123) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permisiunea a fost acordată
            } else {
                Toast.makeText(this, "Permisiunea pentru notificări a fost refuzată", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        handler.removeCallbacksAndMessages(null)
    }









    private fun checkForAppUpdate() {
        println("=== UPDATE CHECK START ===")
        println("📱 Local version: ${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("🔄 Creating AppUpdater...")
                val updater = AppUpdater(this@MainActivity)

                println("📡 Checking for update...")
                val updateInfo = updater.checkForUpdate(showToast = false)

                if (updateInfo != null) {
                    println("🎉 UPDATE FOUND! v${updateInfo.latestVersionName}")

                    // Afișează dialogul pe thread-ul principal
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(updateInfo)
                    }
                } else {
                    println("📭 No update available or error")
                }

            } catch (e: Exception) {
                println("🔥 ERROR in update check: ${e.message}")
                e.printStackTrace()
            }
            println("=== UPDATE CHECK END ===")
        }
    }














    private fun tryFallbackWithProxy() {
        Thread {
            try {
                // Folosește un URL intermediar
                val proxyUrl = "https://api.allorigins.win/raw?url=${java.net.URLEncoder.encode(
                    "https://cdn-update.adelin.org/jellyseerr/app/update.json", "UTF-8")}"

                println("🔄 Trying proxy: $proxyUrl")
                val text = java.net.URL(proxyUrl).readText()
                println("🔄 PROXY SUCCESS: ${text.take(200)}")

                // ... parsează JSON și arată dialog ...

            } catch (e: Exception) {
                println("🔄 PROXY FAILED: ${e.message}")
            }
        }.start()
    }










    private fun showUpdateDialog(updateInfo: AppUpdater.UpdateInfo) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Actualizare disponibilă! v${updateInfo.latestVersionName}")
            .setMessage("Ce este nou:\n${updateInfo.whatsNew}")
            .setPositiveButton("Actualizează acum") { dialog, _ ->
                dialog.dismiss()
                CoroutineScope(Dispatchers.IO).launch {
                    val updater = AppUpdater(this@MainActivity)
                    withContext(Dispatchers.Main) {
                        updater.downloadAndInstall(updateInfo.apkUrl)
                    }
                }
            }
            .setNegativeButton("Mai târziu") { dialog, _ ->
                dialog.dismiss()
                if (updateInfo.mandatory) {
                    // Dacă e actualizare obligatorie, poți forța ieșirea
                    finish()
                }
            }
            .setCancelable(!updateInfo.mandatory)
            .create()

        dialog.show()

        // Personalizare culori (opțional) - folosește culoarea ta
        dialog.window?.setBackgroundDrawableResource(android.R.color.background_dark)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#6D28D9"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#9CA3AF"))

        // Setează culoare text pentru titlu și mesaj
        try {
            val titleTextView = dialog.findViewById<android.widget.TextView>(android.R.id.title)
            val messageTextView = dialog.findViewById<android.widget.TextView>(android.R.id.message)

            titleTextView?.setTextColor(Color.WHITE)
            messageTextView?.setTextColor(Color.WHITE)
        } catch (e: Exception) {
            // Ignoră eroare
        }
    }
}