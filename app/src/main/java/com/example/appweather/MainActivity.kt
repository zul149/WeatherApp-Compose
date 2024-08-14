package com.example.appweather

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.appweather.data.WeatherModel
import com.example.appweather.screens.DialogSearch
import com.example.appweather.screens.MainCard
import com.example.appweather.screens.TabLayout
import com.example.appweather.ui.theme.AppWeatherTheme
import org.json.JSONObject

const val APY_KEY = "e4433acd3f734e89b50141149240506"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppWeatherTheme {
                val daysList = remember { // для списка
                    mutableStateOf(listOf<WeatherModel>())
                }
                val dialogState = remember { // для диалогового окна по кнопке
                    mutableStateOf(false)
                }

                val currentDay = remember { // для карточки дня
                    mutableStateOf(WeatherModel(
                            "",
                            "",
                            "0.0",
                            "",
                            "",
                            "0.0",
                            "0.0",
                            ""
                            ))
                }

                if (dialogState.value) {
                    DialogSearch(dialogState, onSubmit = {
                        getData(it, this, daysList, currentDay)
                    })
                }

                getData("Moscow", this, daysList, currentDay)

                Image(
                    painter = painterResource(id = R.drawable.weather),
                    contentDescription = "img",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.8f), // прозрачность
                    contentScale = ContentScale.Crop
                )
                Column {
                    MainCard(currentDay,
                        onClickSync = {
                        getData("Moscow", this@MainActivity, daysList, currentDay)
                    },
                        onClickSesrch = {
                        dialogState.value = true // показываем
                    }
                    )
                    TabLayout(daysList, currentDay)
                }
            }
        }
    }
}

private fun getData(city: String, context: Context,
                    daysList: MutableState<List<WeatherModel>>,
                    currentDay: MutableState<WeatherModel>) {
    val url = "https://api.weatherapi.com/v1/forecast.json?key=$APY_KEY" +
            "&q=$city" +
            "&days=" +
            "3" +
            "&aqi=no&alerts=no"
    val queue = Volley.newRequestQueue(context)
    val stringRequest = StringRequest( // запрос
        Request.Method.GET,
        url,
        {
                response ->
                val list = getWeatherByDays(response)
                daysList.value = list
                currentDay.value = list[0] //сегодняшний день
        },
        {
                error ->
//            Log.d("MyLog", "Error: $it")
        }
    )
    queue.add(stringRequest)
}

private fun getWeatherByDays(response: String): List<WeatherModel> {
    if (response.isEmpty()) return listOf()
    val list = ArrayList<WeatherModel>()
    val mainObject = JSONObject(response)
    val city = mainObject.getJSONObject("location").getString("name")
    val days = mainObject.getJSONObject("forecast").getJSONArray("forecastday")

    for (i in 0 until days.length()) {
        val item = days[i] as JSONObject //берем три дня по очереди по позиции с 0 по 2
        list.add(
            WeatherModel(
                city,
                item.getString("date"),
                "",
                item.getJSONObject("day").getJSONObject("condition").getString("text"),
                item.getJSONObject("day").getJSONObject("condition").getString("icon"),
                item.getJSONObject("day").getString("maxtemp_c"),
                item.getJSONObject("day").getString("mintemp_c"),
                item.getJSONArray("hour").toString()
            )
        )
    }
    list[0] = list[0].copy( //изменяем наш список
        time = mainObject.getJSONObject("current").getString("last_updated"),
        currentTemp = mainObject.getJSONObject("current").getString("temp_c")
    )
    return list
}
