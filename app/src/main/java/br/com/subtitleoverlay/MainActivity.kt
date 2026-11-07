package br.com.subtitleoverlay

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private var selected: Uri? = null
    private lateinit var status: TextView
    private lateinit var delaySpinner: Spinner
    private lateinit var offsetInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = getPreferences(MODE_PRIVATE).getString("srt_uri", null)?.let { Uri.parse(it) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 28, 40, 28)
            setBackgroundColor(Color.rgb(16, 25, 39))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        fun paragraph(value: String, fontSize: Float = 19f, color: Int = Color.WHITE): TextView {
            return TextView(this).apply {
                text = value; textSize = fontSize; setTextColor(color)
                setPadding(0, 6, 0, 15)
                root.addView(this)
            }
        }
        fun button(label: String, click: () -> Unit) {
            val b = Button(this).apply { text = label; isAllCaps = false; setOnClickListener { click() } }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 8
            root.addView(b, lp)
        }
        paragraph("SUBTITLE OVERLAY TV", 28f)
        paragraph("Legenda externa em português por cima de outros aplicativos. Protótipo experimental para Google TV.", 16f)
        status = paragraph("", 17f, Color.rgb(194, 225, 255))
        button("1. Selecionar arquivo .srt") {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivityForResult(i, 22)
        }
        button("2. Autorizar exibição sobre outros apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
        paragraph("Atraso inicial: tempo para voltar à UniTV e iniciar/retomar o vídeo.", 16f)
        delaySpinner = Spinner(this)
        val options = listOf("5 segundos", "10 segundos", "15 segundos", "20 segundos", "30 segundos", "45 segundos")
        delaySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        delaySpinner.setSelection(2)
        root.addView(delaySpinner)
        button("3. INICIAR LEGENDAS") {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Conceda primeiro a permissão de sobreposição", Toast.LENGTH_LONG).show()
                return@button
            }
            val uri = selected
            if (uri == null) {
                Toast.makeText(this, "Selecione primeiro uma legenda .srt", Toast.LENGTH_LONG).show()
                return@button
            }
            val delays = intArrayOf(5, 10, 15, 20, 30, 45)
            send(SubtitleService.ACTION_START) {
                putExtra(SubtitleService.EXTRA_URI, uri.toString())
                putExtra(SubtitleService.EXTRA_DELAY, delays[delaySpinner.selectedItemPosition])
            }
            Toast.makeText(this, "Volte à UniTV e inicie o vídeo quando o atraso acabar", Toast.LENGTH_LONG).show()
        }
        paragraph("Ajuste fino: se a legenda aparecer antes da fala, use ATRASAR. Se aparecer depois, use ADIANTAR.", 16f)
        offsetInfo = paragraph("", 18f)
        button("Adiantar legenda 5 segundos") { shift(5000) }
        button("Adiantar legenda 1 segundo") { shift(1000) }
        button("Atrasar legenda 1 segundo") { shift(-1000) }
        button("Atrasar legenda 5 segundos") { shift(-5000) }
        button("Pausar / retomar relógio das legendas") { send(SubtitleService.ACTION_PAUSE) }
        button("Parar legendas") { send(SubtitleService.ACTION_STOP) }
        paragraph("Importante: pause o relógio das legendas sempre que pausar a reprodução. A UniTV não compartilha seus controles de reprodução com este aplicativo.", 15f)
        refresh()
    }

    private fun shift(ms: Long) {
        send(SubtitleService.ACTION_SHIFT) { putExtra(SubtitleService.EXTRA_SHIFT, ms) }
        android.os.Handler(mainLooper).postDelayed({ refresh() }, 350L)
    }

    private fun send(action: String, extra: Intent.() -> Unit = {}) {
        val intent = Intent(this, SubtitleService::class.java).setAction(action).apply(extra)
        if (action == SubtitleService.ACTION_START) {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        } else startService(intent)
    }

    private fun refresh() {
        status.text = "Arquivo: ${selected?.lastPathSegment ?: "nenhum"}\nPermissão: ${if (Settings.canDrawOverlays(this)) "autorizada" else "pendente"}"
        val offset = getSharedPreferences(SubtitleService.PREFS, MODE_PRIVATE)
            .getLong(SubtitleService.KEY_OFFSET, 0L)
        offsetInfo.text = "Ajuste atual: ${offset / 1000.0} segundo(s)"
        val error = getSharedPreferences(SubtitleService.PREFS, MODE_PRIVATE).getString("error", null)
        if (error != null) status.append("\nÚltimo erro: $error")
    }

    override fun onResume() { super.onResume(); if (::status.isInitialized) refresh() }

    @Deprecated("Activity result API retained for no-dependency Android TV compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 22 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { /* Current selection remains readable for this session. */ }
            selected = uri
            getPreferences(MODE_PRIVATE).edit().putString("srt_uri", uri.toString()).apply()
            refresh()
        }
    }
}
