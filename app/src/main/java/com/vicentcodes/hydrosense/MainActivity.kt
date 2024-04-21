package com.vicentcodes.hydrosense

import android.animation.LayoutTransition
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.vicentcodes.hydrosense.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var v: ActivityMainBinding
    private var expandedView: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        v = ActivityMainBinding.inflate(layoutInflater)
        setContentView(v.root)
      //  var customFont = Typeface.createFromAsset(assets, "fonts/karla_italic.ttf")
        expandedDefault()
        setupLayout(v.expPH, v.lastValPH, v.cardPH, "ph")
        setupLayout(v.expORP, v.lastValORP, v.cardORP, "orp")
        setupLayout(v.expTEMP, v.lastValTEMP, v.cardTEMP, "temp")
        setupLayout(v.expTDS, v.lastValTDS, v.cardTDS, "tds")
        setupLayout(v.expTurbidity, v.lastValTurbidity, v.cardTurbidity, "tur")

        v.lastValORP.text = "Last read: \n04/Apr 12:00:00"
        v.lastValPH.text = "Last read: \n04/Apr 12:00:00"
        v.lastValTDS.text = "Last read: \n04/Apr 12:00:00"
        v.lastValTEMP.text = "Last read: \n04/Apr 12:00:00"
        v.lastValTurbidity.text = "Last read: \n04/Apr 12:00:00"

        v.valueORP.text = getStyledText("78", "mV")
        v.valuePH.text = getStyledText("7", "")
        v.valueTEMP.text = getStyledText("20", "°C")
        v.valueTDS.text = getStyledText("254", "ppm")
        v.valueTurbidity.text = getStyledText("8", "NTU")

    }

    private fun getStyledText(mainText: String, unitText: String): SpannableString {
        val combinedText = "$mainText$unitText"
        val spannable = SpannableString(combinedText)

        // Ajustar el tamaño y el estilo para las unidades
        val start = mainText.length
        val end = combinedText.length

        // Cambiar el tamaño de las unidades a la mitad del tamaño del texto principal
        spannable.setSpan(RelativeSizeSpan(0.5f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Cambiar el estilo de las unidades a cursiva sans-serif-light thin
        spannable.setSpan(TypefaceSpan("sans-serif-light"), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannable
    }

    private fun expandedDefault() {
        expandedView = v.lastValPH
        v.lastValPH.visibility = View.VISIBLE
        v.lastValORP.visibility = View.GONE
        v.lastValTEMP.visibility = View.GONE
        v.lastValTDS.visibility = View.GONE
        v.lastValTurbidity.visibility = View.GONE
    }
    private fun setupLayout(
        layout: ConstraintLayout,
        detailsView: View,
        expandableButton: View,
        type: String
    ) {
        // Configuración de transición de animación
        layout.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        // Mapa de vista para el texto de cada tipo
        val textViewsMap = mapOf(
            "ph" to listOf(v.txPH, v.valuePH),
            "orp" to listOf(v.txORP, v.valueORP),
            "temp" to listOf(v.txTEMP, v.valueTEMP),
            "tds" to listOf(v.txTDS, v.valueTDS),
            "tur" to listOf(v.txTurbidity, v.valueTurbidity)
        )

        val txExpandedTextSize = 32f
        val txNormalTextSize = 22f
        val valueExpandedTextSize = 36f
        val valueNormalTextSize = 26f

        expandableButton.setOnClickListener {
            // Verificar si la vista ya está expandida
            if (detailsView == expandedView) {
                // Si la vista ya está expandida, no hacer nada
                return@setOnClickListener
            }

            // Reducir todas las vistas a tamaño normal según su prefijo
            textViewsMap.forEach { (_, views) ->
                views.forEach { textView ->
                    val resourceName = textView.resources.getResourceEntryName(textView.id)

                    if (resourceName.startsWith("tx")) {
                        textView.textSize = txNormalTextSize
                    } else if (resourceName.startsWith("value")) {
                        textView.textSize = valueNormalTextSize
                    }
                }
            }

            // Si hay una vista expandida, ocultarla
            expandedView?.visibility = View.GONE

            // Expandir la vista correspondiente y ajustar el tamaño del texto
            textViewsMap[type]?.forEach { textView ->
                val resourceName = textView.resources.getResourceEntryName(textView.id)

                if (resourceName.startsWith("tx")) {
                    textView.textSize = txExpandedTextSize
                } else if (resourceName.startsWith("value")) {
                    textView.textSize = valueExpandedTextSize
                }
            }

            // Mostrar la vista expandida
            expandedView = detailsView
            detailsView.visibility = View.VISIBLE
        }
    }





}
