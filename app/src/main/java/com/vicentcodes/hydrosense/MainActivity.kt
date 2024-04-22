package com.vicentcodes.hydrosense

import android.animation.LayoutTransition
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.chip.Chip
import com.vicentcodes.hydrosense.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import com.github.mikephil.charting.data.Entry as Entry1

class MainActivity : AppCompatActivity() {

    private var url = "https://hydrosense.vercel.app/api?UID=X3P8IYRxHfXYWou7fiQ0AQrZ5ZN2&filtro=top10"
    private lateinit var requestQueue: RequestQueue

    private lateinit var chart: LineChart

    private var json:JSONArray = JSONArray()

    private val TIME_RANGES = mapOf(
        "5min" to 5 * 60 * 1000,
        "3hours" to 3 * 60 * 60 * 1000,
        "12hours" to 12 * 60 * 60 * 1000,
        "20min" to 20 * 60 * 1000,
        "1day" to 24 * 60 * 60 * 1000
    )

    val sensorMap = mapOf(
        R.id.chipPH to "ph",
        R.id.chipORP to "orp",
        R.id.chipTEMP to "temp",
        R.id.chipTDS to "tds",
        R.id.chipTurbidity to "tur"
    )



    private val selectedRange = "20min"

    private val pHList = ArrayList<Double>()
    private val timeList = ArrayList<String>()

    lateinit var v: ActivityMainBinding
    private var expandedView: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.v = ActivityMainBinding.inflate(layoutInflater)
        setContentView(this.v.root)
        //  var customFont = Typeface.createFromAsset(assets, "fonts/karla_italic.ttf")
        expandedDefault()
        setupLayout(this.v.expPH, this.v.lastValPH, this.v.cardPH, "ph")
        setupLayout(this.v.expORP, this.v.lastValORP, this.v.cardORP, "orp")
        setupLayout(this.v.expTEMP, this.v.lastValTEMP, this.v.cardTEMP, "temp")
        setupLayout(this.v.expTDS, this.v.lastValTDS, this.v.cardTDS, "tds")
        setupLayout(this.v.expTurbidity, this.v.lastValTurbidity, this.v.cardTurbidity, "tur")

        this.v.lastValORP.text = "Last read: \n04/Apr 12:00:00"
        this.v.lastValPH.text = "Last read: \n04/Apr 12:00:00"
        this.v.lastValTDS.text = "Last read: \n04/Apr 12:00:00"
        this.v.lastValTEMP.text = "Last read: \n04/Apr 12:00:00"
        this.v.lastValTurbidity.text = "Last read: \n04/Apr 12:00:00"

        this.v.valueORP.text = getStyledText("78", "mV")
        this.v.valuePH.text = getStyledText("7", "")
        this.v.valueTEMP.text = getStyledText("20", "°C")
        this.v.valueTDS.text = getStyledText("254", "ppm")
        this.v.valueTurbidity.text = getStyledText("8", "NTU")


        requestQueue = Volley.newRequestQueue(this)



        refreshData()

        v.chipPH.setOnClickListener {
            getData(selectedRange, "ph")
        }
        v.chipORP.setOnClickListener {
            getData(selectedRange, "orp")
        }
        v.chipTEMP.setOnClickListener {
            getData(selectedRange, "temp")
        }
        v.chipTDS.setOnClickListener {
            getData(selectedRange, "tds")
        }
        v.chipTurbidity.setOnClickListener {
            getData(selectedRange, "turb")
        }
            if(v.chipPH.isChecked) {
                getData(selectedRange, "ph")
            }

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
        expandedView = this.v.lastValPH
        this.v.lastValPH.visibility = View.VISIBLE
        this.v.lastValORP.visibility = View.GONE
        this.v.lastValTEMP.visibility = View.GONE
        this.v.lastValTDS.visibility = View.GONE
        this.v.lastValTurbidity.visibility = View.GONE
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
            "ph" to listOf(this.v.txPH, this.v.valuePH),
            "orp" to listOf(this.v.txORP, this.v.valueORP),
            "temp" to listOf(this.v.txTEMP, this.v.valueTEMP),
            "tds" to listOf(this.v.txTDS, this.v.valueTDS),
            "tur" to listOf(this.v.txTurbidity, this.v.valueTurbidity)
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


    private fun refreshData() {
        this.v.swipeRefreshLayout.setOnRefreshListener {
            getData(selectedRange, "ph")

            this.v.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun getData(filterTime: String, sensor: String) {
        val currentTimeMillis = System.currentTimeMillis()
        val selectedTimeRange = TIME_RANGES[filterTime] ?: 3 * 60 * 60 * 1000 // 3 hours by default
        val startTime = currentTimeMillis - selectedTimeRange

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                try {


                   /* pHList.clear()
                    timeList.clear()

                    val filteredData = mutableListOf<Pair<Long, Double>>()
                    for (i in response.length() - 1 downTo 0) {
                        val jsonObject = response.getJSONObject(i)
                        val fecha = jsonObject.getLong("date")
                        val ph = jsonObject.getDouble(sensor)

                        if (fecha >= startTime) {
                            filteredData.add(Pair(fecha, ph))
                        }
                    }

                    if (filteredData.isNotEmpty()) {
                        filteredData.sortBy { it.first }
                        for (data in filteredData) {
                            pHList.add(data.second)
                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val hora = sdf.format(Date(data.first))
                            timeList.add(hora)
                        }

                        chart = this.v.chart
                        setupLineChart()
                        //setupChips()
                    } else {
                        // No data in the selected range, get the oldest data
                        val oldestData = mutableListOf<Pair<Long, Double>>()
                        for (i in response.length() - 1 downTo 0) {
                            val jsonObject = response.getJSONObject(i)
                            val fecha = jsonObject.getLong("date")
                            val ph = jsonObject.getDouble(sensor)
                            oldestData.add(Pair(fecha, ph))
                        }
                        if (oldestData.isNotEmpty()) {
                            oldestData.sortBy { it.first }
                            val lastItems = oldestData.takeLast(10)
                            for (data in lastItems) {
                                pHList.add(data.second)
                                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val hora = sdf.format(Date(data.first))
                                timeList.add(hora)
                            }
                            chart = this.v.chart
                            setupLineChart()
                            setupChips()
                        } else {
                            Toast.makeText(this,
                                getString(R.string.noDataAvitable), Toast.LENGTH_SHORT).show()
                        }
                    }

                    */
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                error.printStackTrace()
            }
        )

        requestQueue.add(jsonArrayRequest)
    }

    private fun setupLineChart() {



        val lineDataSet = LineDataSet(getEntryValues(), "")
        lineDataSet.color = ContextCompat.getColor(this, R.color.background)
        lineDataSet.setDrawCircles(false)
        lineDataSet.fillDrawable = ContextCompat.getDrawable(this, R.drawable.gradient_background)

        lineDataSet.highlightLineWidth = 0.5f

        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineDataSet.cubicIntensity = 0.2f
        lineDataSet.color = ContextCompat.getColor(this, R.color.black)

        val dataSets = arrayListOf(lineDataSet)
        val data = LineData(dataSets as List<ILineDataSet>?)

        chart.apply {
            this.data = data
            setDrawMarkers(false)
            description = Description().apply {
                text = ""
            }
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.background))
            buildXAxis(xAxis)
            axisLeft.apply {
                textColor = Color.WHITE
            }
            lineDataSet.valueTextColor = Color.WHITE
            lineDataSet.setDrawValues(false)
            axisRight.isEnabled = false
            chart.setDrawMarkers(false)
            lineDataSet.setDrawFilled(true)
            animateXY(500, 1200)
            //buildLegend(legend)
            legend.isEnabled = false
            invalidate()
        }

        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry1?, h: Highlight?) {

                v.valueGraph.text = "${e?.y} a las ${timeList[e?.x?.toInt() ?: 0]}"
            }

            override fun onNothingSelected() {

                v.valueGraph.text = ""
            }
        })
    }

    private fun buildXAxis(xAxis: XAxis) {
        xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < timeList.size) {
                        timeList[index]
                    } else {
                        ""
                    }
                }
            }
            granularity = 1f
            textSize = 12f
            textColor = Color.WHITE
            position = XAxis.XAxisPosition.BOTTOM
        }
    }

    private fun buildLegend(legend: Legend) {
        legend.apply {
            textColor = Color.WHITE
            textSize = 16f
            form = Legend.LegendForm.LINE
        }
    }

    private fun setupChips() {
        for (view in this.v.chipGroup.children) {
            val chip = view as Chip
            chip.setOnCheckedChangeListener { _, _ ->
                val index = this.v.chipGroup.indexOfChild(view)
                this.v.chipGroup.removeView(view)
                this.v.chipGroup.addView(view, index)
            }
        }

        this.v.chart.setOnLongClickListener {
            chart.fitScreen()
            true
        }
    }

    private fun getEntryValues(): MutableList<Entry1> {
        val entries = mutableListOf<Entry1>()

        for (i in pHList.indices) {
            entries.add(Entry1(i.toFloat(), pHList[i].toFloat()))
        }

        return entries
    }




}
