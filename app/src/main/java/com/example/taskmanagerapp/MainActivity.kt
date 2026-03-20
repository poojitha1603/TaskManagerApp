package com.example.taskmanagerapp

// ✅ IMPORTS
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.viewinterop.AndroidView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 🔥 WORKMANAGER IMPORTS
import androidx.work.*
import java.util.concurrent.TimeUnit

// ✅ MAIN ACTIVITY
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔔 REQUEST NOTIFICATION PERMISSION
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            App()
        }
    }
}

// ✅ APP NAVIGATION
@Composable
fun App() {

    val context = LocalContext.current
    val dataStore = DataStoreManager(context)
    val scope = rememberCoroutineScope()

    var isLoggedIn by remember { mutableStateOf(false) }
    var showWeb by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dataStore.getUsername.collect {
            if (it != null) {
                isLoggedIn = true
            }
        }
    }

    when {
        !isLoggedIn -> {
            LoginScreen { username ->
                scope.launch {
                    dataStore.saveUsername(username)
                    isLoggedIn = true
                }
            }
        }

        showWeb -> {
            WebViewScreen()
        }

        else -> {
            DashboardScreen(
                onOpenWeb = { showWeb = true },
                onStartReminder = { scheduleReminder(context) }
            )
        }
    }
}

// ✅ LOGIN SCREEN
@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Enter Username") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            if (username.isNotEmpty()) {
                onLogin(username)
            }
        }) {
            Text("Login")
        }
    }
}

// ✅ DASHBOARD
@Composable
fun DashboardScreen(
    onOpenWeb: () -> Unit,
    onStartReminder: () -> Unit
) {

    var tasks by remember { mutableStateOf(listOf<String>()) }
    var showDialog by remember { mutableStateOf(false) }

    // 🔥 API CALL
    LaunchedEffect(Unit) {
        RetrofitInstance.api.getTodos().enqueue(object : Callback<List<Todo>> {
            override fun onResponse(
                call: Call<List<Todo>>,
                response: Response<List<Todo>>
            ) {
                if (response.isSuccessful) {
                    tasks = response.body()?.take(10)?.map { it.title } ?: emptyList()
                }
            }

            override fun onFailure(call: Call<List<Todo>>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {

        Button(onClick = { showDialog = true }) {
            Text("Add Task")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = onOpenWeb) {
            Text("Open Website")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = onStartReminder) {
            Text("Start Reminder 🔔")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(tasks) {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (showDialog) {
        AddTaskDialog(
            onAdd = { newTask ->
                tasks = tasks + newTask
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

// ✅ DIALOG
@Composable
fun AddTaskDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var taskText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task") },
        text = {
            TextField(
                value = taskText,
                onValueChange = { taskText = it },
                label = { Text("Enter task") }
            )
        },
        confirmButton = {
            Button(onClick = { onAdd(taskText) }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ✅ WEBVIEW
@Composable
fun WebViewScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val webView = WebView(context)

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }

            webView.webViewClient = WebViewClient()
            webView.loadUrl("https://www.google.com")

            webView
        }
    )
}

// 🔥 WORKMANAGER FUNCTION
fun scheduleReminder(context: android.content.Context) {

    val workRequest =
        OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

    WorkManager.getInstance(context).enqueue(workRequest) // ✅ FIXED
}