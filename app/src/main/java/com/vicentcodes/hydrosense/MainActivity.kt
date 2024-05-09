package com.vicentcodes.hydrosense

import android.animation.LayoutTransition
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.vicentcodes.hydrosense.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

data class SensorData(
    val ph: Double?,
    val tds: Double?,
    val temp: Double?,
    val date: Long?,
    val orp: Double?,
    val turb: Double?
)

data class sensorHistoy(
    val fecha: Long,
    val valor: String,
)


class MainActivity : AppCompatActivity() {

    private var selectedSensor = ""
    private var selectedRange = "20min"
    private var url = "https://hydrosense.vercel.app/api?UID=X3P8IYRxHfXYWou7fiQ0AQrZ5ZN2&filtro=top10"
    private lateinit var requestQueue: RequestQueue
    private lateinit var chart: LineChart
    private var expandedView: View? = null

    private var dialog:Dialog? = null

    private val timeRanges = mapOf(
        "30min" to 30 * 60 * 1000,
        "1hour" to 1 * 60 * 60 * 1000,
        "4hours" to 4 * 60 * 60 * 1000,
        "8hours" to 8 * 60 * 60 * 1000,
        "12hours" to 12 * 60 * 60 * 1000,
        "1day" to 24 * 60 * 60 * 1000
    )


    private val timeRangeSpinner:List<String> = listOf("Most recent", "1 hour", "4 hours", "8 hours", "12 hours", "1 day")


    private val pHList = ArrayList<Double>()
    private val timeList = ArrayList<String>()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialView()
        initializeLayout()


        requestQueue = Volley.newRequestQueue(this)

        setupChipListeners()
        setupSwipeRefresh()

        binding.spinnerRangeTime22.setText(timeRangeSpinner[0])

        if (binding.chipPH.isChecked) {
            selectedSensor = "ph"
            getData(timeRangeSpinner[0], selectedSensor, false)
        }


        binding.chart.setOnLongClickListener {

            chart.fitScreen()
           Snackbar.make(findViewById(R.id.main), "Ultima actualizacion: ${lastTimeUpdate()}", Snackbar.LENGTH_SHORT).show()
            true
        }


        // Crear el adaptador
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item, // Estilo básico para mostrar el texto
            timeRangeSpinner // Lista de claves del mapa
        )

        // Establecer el adaptador en el AutoCompleteTextView
        val spinnerRangeTime = findViewById<AutoCompleteTextView>(R.id.spinnerRangeTime22)
        spinnerRangeTime.setAdapter(adapter)

        binding.spinnerRangeTimeParent22.setBoxBackgroundColorResource(R.color.background)

        binding.spinnerRangeTime22.setOnItemClickListener { parent, _, position, _ ->
            val selectedRange = parent.getItemAtPosition(position) as String
            getData(selectedRange, selectedSensor, true)
        }


        binding.showChart.setOnClickListener {
           dialog = Dialog(this)
            dialog?.setContentView(R.layout.expanded_chart_dialog)
            //dialog on full screen or match parent
            dialog?.window?.setLayout(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
            dialog?.show()

            val exitButton = dialog?.findViewById<ImageView>(R.id.close)
            val rvList = dialog?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
            val groupChip = dialog?.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroup)
            val chipPH = dialog?.findViewById<com.google.android.material.chip.Chip>(R.id.chipPHd)
            val chipORP = dialog?.findViewById<com.google.android.material.chip.Chip>(R.id.chipORPd)
            val chipTEMP = dialog?.findViewById<com.google.android.material.chip.Chip>(R.id.chipTEMPd)
            val chipTDS = dialog?.findViewById<com.google.android.material.chip.Chip>(R.id.chipTDSd)
            val chipTurbidity = dialog?.findViewById<com.google.android.material.chip.Chip>(R.id.chipTurbidityd)

            updateRecyclerView(selectedSensor, rvList!!)



            groupChip?.clearCheck()
            if (selectedSensor == "ph") {
                chipPH?.isChecked = true
            }
            if (selectedSensor == "orp") {
                chipORP?.isChecked = true
            }
            if (selectedSensor == "temp") {
                chipTEMP?.isChecked = true
            }
            if (selectedSensor == "tds") {
                chipTDS?.isChecked = true
                }
            if (selectedSensor == "tur") {
                chipTurbidity?.isChecked = true
            }


            chipPH?.setOnClickListener {
                selectedSensor = "ph"
                updateRecyclerView(selectedSensor, rvList!!)
            }

            chipTEMP?.setOnClickListener {
                selectedSensor = "temp"
                updateRecyclerView(selectedSensor, rvList!!)
            }
            chipTDS?.setOnClickListener {
                selectedSensor = "tds"
                updateRecyclerView(selectedSensor, rvList!!)
            }




            exitButton?.setOnClickListener {
                dialog?.dismiss()
            }

        }










    }

    private fun setupInitialView() {
        expandedView = binding.lastValPH
        binding.apply {
            lastValPH.visibility = View.VISIBLE
            lastValORP.visibility = View.GONE
            lastValTEMP.visibility = View.GONE
            lastValTDS.visibility = View.GONE
            lastValTurbidity.visibility = View.GONE
        }
    }

    private fun updateRecyclerView(sensorType: String, recyclerView: androidx.recyclerview.widget.RecyclerView) {
        val sharedPref = applicationContext.getSharedPreferences("app_cache", Context.MODE_PRIVATE)

        val json = sharedPref.getString("cached_json", null)
        val gson = Gson()
        val listType: Type = object : TypeToken<List<SensorData>>() {}.type
        var sensorDataList: List<SensorData> = gson.fromJson(json, listType)

        val sensorOrder  = sensorDataList.sortedByDescending { it.date }



        val sensorDataToDisplay = when (sensorType) {
            "ph" -> sensorOrder.map { Pair(it.date, it.ph) }
            "temp" -> sensorOrder.map { it.temp?.let { it1 -> Pair(it.date, it1.toDouble()) } }
            "tds" -> sensorOrder.map { it.tds?.let { it1 -> Pair(it.date, it1.toDouble()) } }
            "turb" -> sensorOrder.map { it.turb?.let { it1 -> Pair(it.date, it1.toDouble()) } }
            "orp" -> sensorOrder.map { it.orp?.let { it1 -> Pair(it.date, it1.toDouble()) } }
            else -> emptyList()
        }

        // Crear o actualizar el adapter
        val adapter = SensorDataAdapter(sensorDataToDisplay)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun initializeLayout() {
        setupLayout(binding.expPH, binding.lastValPH, binding.cardPH, "ph")
        setupLayout(binding.expORP, binding.lastValORP, binding.cardORP, "orp")
        setupLayout(binding.expTEMP, binding.lastValTEMP, binding.cardTEMP, "temp")
        setupLayout(binding.expTDS, binding.lastValTDS, binding.cardTDS, "tds")
        setupLayout(binding.expTurbidity, binding.lastValTurbidity, binding.cardTurbidity, "tur")
    }
    private fun formatTimestampToString(timestamp: Long?): String {
        if (timestamp == null) return "Invalid Timestamp"

        // Creamos un objeto Date a partir del timestamp
        val date = Date(timestamp)

        // Definimos el formato deseado
        val dateFormat = SimpleDateFormat("dd/MMM HH:mm", Locale.ENGLISH) // "04/Apr 12:00"

        // Retornamos la fecha formateada
        return dateFormat.format(date)
    }
    private fun setDefaultSensorValues() {
        binding.apply {

            //format getFirstFieldValue("timestamp").toString() a 04/Apr 12:00

            val formatDate = formatTimestampToString(getFirstFieldValue("date").toString().toLong())

            val date = "Last read: \n${formatTimestampToString(getFirstFieldValue("date").toString().toLong())} "
            binding.dayLecture.text = formatDate

            val lastDayUpdated = Date(getFirstFieldValue("date").toString().toLong())

            // Definimos el formato deseado
            val dateFormat = SimpleDateFormat("dd/MMMM ", Locale.ENGLISH) // "04/Apr 12:00"

            // Retornamos la fecha formateada
            binding.dayLecture.text = dateFormat.format(lastDayUpdated)


            lastValORP.text = date

            lastValORP.text = date
            lastValPH.text = date
            lastValTDS.text = date
            lastValTEMP.text = date
            lastValTurbidity.text = date

            valueORP.text = getStyledText(getFirstFieldValue("orp").toString(), "mV")
            valuePH.text = getStyledText(getFirstFieldValue("ph").toString(), "")
            valueTEMP.text = getStyledText(getFirstFieldValue("temp").toString(), "°C")
            valueTDS.text = getStyledText(getFirstFieldValue("tds").toString(), "ppm")
            valueTurbidity.text = getStyledText(getFirstFieldValue("turb").toString(), "NTU")
        }
    }

    private fun setupChipListeners() {
        val chipListeners = mapOf(
            binding.chipPH to "ph",
            binding.chipORP to "orp",
            binding.chipTEMP to "temp",
            binding.chipTDS to "tds",
            binding.chipTurbidity to "tur"
        )

        for ((chip, sensor) in chipListeners) {
            chip.setOnClickListener {
                selectedSensor = sensor
                getData(selectedRange, selectedSensor, false)
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            getData("top10", selectedSensor, true)
            binding.swipeRefreshLayout.isRefreshing = true
        }
    }

    private fun getStyledText(mainText: String, unitText: String): SpannableString {
        val combinedText = "$mainText$unitText"
        val spannable = SpannableString(combinedText)

        val start = mainText.length
        val end = combinedText.length

        spannable.setSpan(RelativeSizeSpan(0.5f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(TypefaceSpan("sans-serif-light"), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannable
    }

    private fun setupLayout(
        layout: ConstraintLayout,
        detailsView: View,
        expandableButton: View,
        type: String
    ) {
        layout.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        expandableButton.setOnClickListener {
            if (detailsView == expandedView) {
                return@setOnClickListener
            }

            val textViewsMap = mapOf(
                "ph" to listOf(binding.txPH, binding.valuePH),
                "orp" to listOf(binding.txORP, binding.valueORP),
                "temp" to listOf(binding.txTEMP, binding.valueTEMP),
                "tds" to listOf(binding.txTDS, binding.valueTDS),
                "tur" to listOf(binding.txTurbidity, binding.valueTurbidity)
            )

            collapseAllTextViews(textViewsMap)

            expandedView?.visibility = View.GONE

            expandTextViews(textViewsMap, type)

            expandedView = detailsView
            detailsView.visibility = View.VISIBLE
        }
    }

    private fun collapseAllTextViews(textViewsMap: Map<String, List<TextView>>) {
        val txNormalTextSize = 22f
        val valueNormalTextSize = 26f

        textViewsMap.forEach { (_, views) ->
            views.forEach { textView ->
                val resourceName = textView.resources.getResourceEntryName(textView.id)

                when {
                    resourceName.startsWith("tx") -> textView.textSize = txNormalTextSize
                    resourceName.startsWith("value") -> textView.textSize = valueNormalTextSize
                }
            }
        }
    }

    private fun expandTextViews(
        textViewsMap: Map<String, List<TextView>>,
        type: String
    ) {
        val txExpandedTextSize = 32f
        val valueExpandedTextSize = 36f

        textViewsMap[type]?.forEach { textView ->
            val resourceName = textView.resources.getResourceEntryName(textView.id)

            when {
                resourceName.startsWith("tx") -> textView.textSize = txExpandedTextSize
                resourceName.startsWith("value") -> textView.textSize = valueExpandedTextSize
            }
        }
    }

    private fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        return networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun saveJsonToSharedPreferences(context: Context, json: JSONArray) {
        val sharedPref = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
        val currentTimeMillis = System.currentTimeMillis()

        with(sharedPref.edit()) {
            putString("cached_json", json.toString())
            putLong("cache_time", currentTimeMillis)
            apply()
        }
    }

    data class CachedData(val json: JSONArray?, val cacheTime: Long)

    private fun loadJsonFromSharedPreferences(context: Context): CachedData {
        val sharedPref = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("cached_json", null)
        val cacheTime = sharedPref.getLong("cache_time", 0L)

        val json = jsonString?.let { JSONArray(it) }

        return CachedData(json, cacheTime)
    }

    private fun getData(
        filterTime: String,
        sensor: String,
        forceNetwork: Boolean
    ) {
        // Remove blank spaces and initialize variables
        val filterTimeForm = filterTime.replace("\\s".toRegex(), "")
        val currentTimeMillis = System.currentTimeMillis()
        val isConnected = isConnectedToInternet(this)
        val cachedData = loadJsonFromSharedPreferences(this)

        if (!isConnected) {
            handleNoConnection()
            return
        }

        val shouldUpdateFromApi = forceNetwork || cachedData.json == null ||
                (filterTimeForm == "Mostrecent" && currentTimeMillis - cachedData.cacheTime > 30 * 60 * 1000)

        // Adjust filterTimeForm if data is outdated for 'Mostrecent'
        val adjustedFilterTime = if (shouldUpdateFromApi && filterTimeForm == "Mostrecent") "top10" else filterTimeForm


         url = "https://hydrosense.vercel.app/api?UID=X3P8IYRxHfXYWou7fiQ0AQrZ5ZN2&filtro=$adjustedFilterTime"

        if (!shouldUpdateFromApi) {
            cachedData.json?.let { manipulateData(it, adjustedFilterTime, sensor) }
            // Snackbar indicating last update
            // Snackbar.make(findViewById(R.id.main), "Datos actualizados Última actualización: ${lastTimeUpdate()}", Snackbar.LENGTH_SHORT).show()
            return
        }

        fetchDataFromNetwork(adjustedFilterTime, sensor)
    }


    private fun lastTimeUpdate(): String {
        val cachedData = loadJsonFromSharedPreferences(this)
        val cacheTime = cachedData.cacheTime

        //show the how many minutes ago the data was updated
        val currentTimeMillis = System.currentTimeMillis()
        val minutesAgo = (currentTimeMillis - cacheTime) / 60000


        return if (minutesAgo == 1L) "hace $minutesAgo minuto" else "hace $minutesAgo minutos"
    }

    private fun handleNoConnection() {
        val cachedData = loadJsonFromSharedPreferences(this)
        val json = cachedData.json

        if (json != null) {

            Snackbar.make(findViewById(R.id.main), "No hay conexión a internet. Última actualización: ${lastTimeUpdate()}", Snackbar.LENGTH_SHORT).show()



            manipulateData(json, selectedRange, selectedSensor)
        } else {
            Snackbar.make(findViewById(R.id.main), "No hay conexión a internet", Snackbar.LENGTH_SHORT).show()

        }
    }

    private fun fetchDataFromNetwork(filterTime: String, sensor: String) {
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    saveJsonToSharedPreferences(this, response)
                    binding.swipeRefreshLayout.isRefreshing = false
                    Snackbar.make(findViewById(R.id.main), "Datos actualizados", Snackbar.LENGTH_SHORT).show()
                    manipulateData(response, filterTime, sensor)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Snackbar.make(findViewById(R.id.main), "Error al obtener datos $e", Snackbar.LENGTH_SHORT).show()
                }
            },
            { error ->
                val networkResponse = error.networkResponse
                val statusCode = networkResponse?.statusCode ?: -1
                if(statusCode == 404){
                    Snackbar.make(findViewById(R.id.main), "No datos para las ultimas $selectedRange", Snackbar.LENGTH_SHORT).show()
                }
            }
        )

        requestQueue.add(jsonArrayRequest)
    }

    private fun manipulateData(
        jsonData: JSONArray,
        filterTime: String,
        sensor: String
    ) {
        pHList.clear()
        timeList.clear()

        val currentTimeMillis = System.currentTimeMillis()
        val selectedTimeRange = timeRanges[filterTime]
            ?: (4 * 60 * 60 * 1000)
        val startTime = currentTimeMillis - selectedTimeRange

        val filteredData = mutableListOf<Pair<Long, Double>>()
        filteredData.sortedByDescending { it.second }

        try {
            for (i in 0 until jsonData.length()) { // Cambiado para recorrer desde el principio
                val jsonObject = jsonData.getJSONObject(i)
                val date = jsonObject.getLong("date")
                val sensorValue = jsonObject.getDouble(sensor)

                if (date in startTime..currentTimeMillis) { // Modificado para el rango completo
                    filteredData.add(Pair(date, sensorValue))
                }
            }

            filteredData.sortedByDescending { it.second }


            if (filteredData.isNotEmpty()) {
                processFilteredData(filteredData)
            } else {
                handleNoData(jsonData, sensor)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }


        setDefaultSensorValues()
    }

    private fun processFilteredData(
        filteredData: MutableList<Pair<Long, Double>>
    ) {
        // Ordena los datos por tiempo
        filteredData.sortByDescending{ it.second }

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (data in filteredData) {
            pHList.add(data.second)
            timeList.add(sdf.format(Date(data.first)))
        }

        setupChart() // Reconfigura el gráfico con los nuevos datos
    }

    private fun handleNoData(
        jsonData: JSONArray,
        sensor: String
    ) {
        val oldestData = mutableListOf<Pair<Long, Double>>()

        for (i in jsonData.length() - 1 downTo 0) {
            val jsonObject = jsonData.getJSONObject(i)
            val fecha = jsonObject.getLong("date")
            val ph = jsonObject.getDouble(sensor)

            oldestData.add(Pair(fecha, ph))
        }

        if (oldestData.isNotEmpty()) {
            val lastItems = oldestData.takeLast(10)
            oldestData.sortBy { it.first }

            for (data in lastItems) {
                pHList.add(data.second)
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                timeList.add(sdf.format(Date(data.first)))
            }

            setupChart()
        } else {
            /*Toast.makeText(
                this,
                getString(R.string.noDataAvailable),
                Toast.LENGTH_SHORT
            ).show()*/
        }
    }

    private fun setupChart() {
        chart = binding.chart

        val lineDataSet = LineDataSet(getEntryValues() as List<Entry>?, "").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.PH)
            setDrawCircles(false)
            fillDrawable = ContextCompat.getDrawable(this@MainActivity, R.drawable.gradient_background)
            highlightLineWidth = 0.5f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawFilled(true)
            setDrawValues(false)
        }

        chart.apply {
            data = LineData(listOf(lineDataSet))
            setDrawMarkers(false)
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.background))
            description.text = ""
            buildXAxis(xAxis)
            legend.isEnabled = false
            axisLeft.apply {
                textColor = Color.WHITE
            }
            axisRight.isEnabled = false
            animateXY(300, 500)
            invalidate()
        }

        binding.minMaxValue.text = "${getMinMax(selectedSensor).first} - ${getMinMax(selectedSensor).second}"

       // binding.minMaxTitle.text = "Min - Max ${getMinMaxWithTime(selectedSensor).second.first} - ${getMinMaxWithTime(selectedSensor).second.second}"

        binding.minMaxTitle.text = "Min - Max"

        startAutoUpdate()

        /*chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                binding.valueGraph.text = "${e?.y} a las ${timeList[e?.x?.toInt() ?: 0]}"
            }

            override fun onNothingSelected() {
                binding.valueGraph.text = ""
            }
        })*/
    }
    private fun startAutoUpdate() {
        // Usamos lifecycleScope para gestionar el ciclo de vida de la corrutina
        lifecycleScope.launch {
            while (isActive) { // 'isActive' se controla por el ciclo de vida de la Activity o Fragment
                // Actualizamos el texto del TextView
                binding.lastUpdateRead.text = "Última actualización: \n${lastTimeUpdate()}"

                // Esperamos un minuto antes de la próxima actualización
                delay(60000) // 60,000 milisegundos equivalen a 1 minuto
            }
        }
    }
    private fun buildXAxis(xAxis: XAxis) {
        val reversedTimeList = timeList.reversed() // Invertimos las etiquetas de tiempo

        xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < reversedTimeList.size) {
                        reversedTimeList[index] // Devuelve la etiqueta correspondiente
                    } else {
                        ""
                    }
                }
            }
            granularity = 1f // Asegura que se muestren todas las etiquetas
            textSize = 12f
            textColor = Color.WHITE
            position = XAxis.XAxisPosition.BOTTOM
        }
    }


    fun getMinMaxWithTime(sensor: String): Pair<Pair<Double, Double>, Pair<String, String>> {
        val json = loadJsonFromSharedPreferences(this).json ?: JSONArray()

        var min = POSITIVE_INFINITY
        var max = NEGATIVE_INFINITY

        var minDate: Long? = null
        var maxDate: Long? = null

        // Iterar sobre el JSONArray
        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            // Verificar si el objeto contiene el sensor deseado
            if (item.has(sensor) && item.has("date")) {
                val value = item.getDouble(sensor)
                val date = item.getLong("date")

                // Actualizar el mínimo y su fecha asociada
                if (value < min) {
                    min = value
                    minDate = date
                }

                // Actualizar el máximo y su fecha asociada
                if (value > max) {
                    max = value
                    maxDate = date
                }
            }
        }

        // Asegurarse de no devolver valores nulos
        minDate = minDate ?: 0L
        maxDate = maxDate ?: 0L

        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        var formattedMinDate = minDate.let { dateFormat.format(Date(it)) } ?: "N/A"
        var formattedMaxDate = maxDate.let { dateFormat.format(Date(it)) } ?: "N/A"

        //if min and max are diferent day from today also show the day
        if (formattedMinDate != "N/A" && formattedMaxDate != "N/A") {
            val minDateFormatted = Date(minDate)
            val maxDateFormatted = Date(maxDate)

            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            val minDateFormattedString = sdf.format(minDateFormatted)
            val maxDateFormattedString = sdf.format(maxDateFormatted)

            if (minDateFormattedString != maxDateFormattedString) {
                val sdf2 = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                formattedMinDate = sdf2.format(minDateFormatted)
                formattedMaxDate = sdf2.format(maxDateFormatted)
            }
        }

        return Pair(Pair(min, max), Pair(formattedMaxDate, formattedMinDate))
    }


    private fun getFirstFieldValue(sensorName: String): Any? {
        // Usamos Gson para convertir el JSON a una lista de objetos SensorData

        val json = loadJsonFromSharedPreferences(this).json ?: JSONArray()

        val gson = Gson()
        val listType: Type = object : TypeToken<List<SensorData>>() {}.type
        val sensorDataList: List<SensorData> = gson.fromJson(json.toString(), listType)

        // Tomamos el primer objeto de la lista
        val firstData = sensorDataList.firstOrNull() ?: return null
        val decimalFormat = DecimalFormat("#.#")

        fun formatIfNeeded(value: Double?): Any? {
            if (value == null) return null
            return if (value % 1 == 0.0) { // Es entero
                value.toInt() // Retornar como entero
            } else {
                decimalFormat.format(value).toDouble() // Formatear a un decimal
            }
        }
        // Usamos reflexión para obtener el valor del campo solicitado
        return when (sensorName) {
            "ph" -> formatIfNeeded(firstData.ph)
            "tds" -> formatIfNeeded(firstData.tds)
            "temp" -> formatIfNeeded(firstData.temp)
            "orp" -> formatIfNeeded(firstData.orp)
            "turb" -> formatIfNeeded(firstData.turb)
            "date" -> firstData.date  // Convertimos a Date para campos de tiempo
            else -> null
        }
    }


    private fun getMinMax(sensor: String): Pair<Double, Double> {
        val json = loadJsonFromSharedPreferences(this).json ?: JSONArray()

        var min = POSITIVE_INFINITY
        var max = NEGATIVE_INFINITY

        // Iterar sobre el JSONArray
        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            // Verificar si el objeto contiene el sensor deseado
            if (item.has(sensor)) {
                val value = item.getDouble(sensor)

                // Actualizar el mínimo y máximo
                if (value < min) {
                    min = value
                }

                if (value > max) {
                    max = value
                }
            }
        }

        if (min % 1 != 0.0) {
            min = String.format("%.1f", min).toDouble()
        }
        if (max % 1 != 0.0) {
            max = String.format("%.1f", max).toDouble()
        }


        return Pair(min, max)
    }


    private fun getEntryValues(): MutableList<Entry> {
        val entries = mutableListOf<Entry>()

        // Invertimos el orden de pHList para que el más reciente sea el primero
        val reversedPHList = pHList.reversed()

        // Añadimos los elementos al listado de entradas en el orden invertido
        for (i in reversedPHList.indices) {
            entries.add(Entry(i.toFloat(), reversedPHList[i].toFloat()))
        }

        return entries
    }

}