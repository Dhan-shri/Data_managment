package com.example.littlelemon

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.littlelemon.ui.theme.LittleLemonTheme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(contentType = ContentType("text", "plain"))
        }
    }

    private val menuItemsLiveData =  MutableLiveData<List<MenuItemNetwork>>()

    private val database by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database").build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            if (database.menuItemDao().isEmpty()) {
                // add code here
                val menuItems = fetchMenu()

                runOnUiThread {
                    menuItemsLiveData.value = menuItems
                }
                saveMenuToDatabase(menuItems)
            }
        }
        setContent {
            LittleLemonTheme {
                // add databaseMenuItems code here

                val menuItems by database.menuItemDao().getAll().observeAsState(emptyList())
                // add orderMenuItems variable here

                var orderMenuItems by remember { mutableStateOf(false) }
                var searchPhrase by remember { mutableStateOf("") }


                // add menuItems variable here
                var menuItem = if (orderMenuItems) {
                    menuItems.sortedBy { it.title }
                } else {
                    menuItems
                }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "logo",
                        modifier = Modifier.padding(50.dp)
                    )

                    // add Button code here
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { orderMenuItems = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = "Tap to Order By Name")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchPhrase,
                        onValueChange = { newSearchPhrase ->
                            searchPhrase = newSearchPhrase
                            if (newSearchPhrase.isNotEmpty()) {
                                // Filter menu items if search phrase is not empty
                                menuItem = menuItems.filter { it.title.contains(newSearchPhrase, ignoreCase = true) }
                            }
                        },
                        label = { Text("Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 50.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MenuItemsList(menuItem,  searchPhrase)

                    // add searchPhrase variable here

                    // Add OutlinedTextField

                    // add is not empty check here
//                    MenuItemsList(items = menuItems)
                }
            }
        }


    }

    private suspend fun fetchMenu(): List<MenuItemNetwork> {
        val response:  MenuNetwork =
            httpClient.get("https://raw.githubusercontent.com/Meta-Mobile-Developer-PC/Working-With-Data-API/main/littleLemonSimpleMenu.json").body()


        Log.d("MainActivity", "fetchMenu: response = $response")
        return response.menu
        // data URL: https://raw.githubusercontent.com/Meta-Mobile-Developer-PC/Working-With-Data-API/main/littleLemonSimpleMenu.json
    }

    private fun saveMenuToDatabase(menuItemsNetwork: List<MenuItemNetwork>) {
        val menuItemsRoom = menuItemsNetwork.map { it.toMenuItemRoom() }
        database.menuItemDao().insertAll(*menuItemsRoom.toTypedArray())
    }
}


@Composable
private fun MenuItemsList(items: List<MenuItemRoom>, searchPhrase: String) {
    val filteredMenuItems = if(searchPhrase.isNotBlank()){
        items.filter { it.title.contains(searchPhrase, ignoreCase = true) }
    } else {
        items
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 20.dp)
    ) {
        items(
            items = filteredMenuItems,
            itemContent = { menuItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
//                    for (item in filteredMenuItems) {
                        Text(menuItem.title)
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .padding(5.dp),
                            textAlign = TextAlign.Right,
                            text = "%.2f".format(menuItem.price)
                        )
//                    }
                }
            }
        )
    }
}
