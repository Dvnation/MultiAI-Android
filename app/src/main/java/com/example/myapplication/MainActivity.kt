//package com.example.myapplication




package com.example.myapplication // Change this to match your actual package name

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlin.math.round


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorScreen()
        }
    }
}

@Composable
fun CalculatorScreen() {
    // State variables to hold the numbers and result
    var number1 by remember { mutableStateOf("") }
    var number2 by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var enabler by remember { mutableStateOf(true) }
    var add by remember { mutableStateOf(true) }
    var multiply by remember { mutableStateOf(false) }
    var subtract by remember { mutableStateOf(false) }
    var divide by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf("") }
    val focus1 = remember { FocusRequester() }
    var keyboard = LocalSoftwareKeyboardController.current
    var focusManager = LocalFocusManager.current

    val scrolls = rememberScrollState()

//    LaunchedEffect(Unit) {
//        scrolls.scrollTo(0)
//    }


    // Column stacks items vertically
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Light gray background styling
            .verticalScroll(scrolls)
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly


        ) {
            Text("TEXT1", color = Color.Blue)
            Text("TEXT2", color = Color.Red)
            Text("TEXT3", color = Color.Black)

        }

//rovali row= vertical alignment
        Column(

            modifier = Modifier
                .fillMaxWidth()

                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),


            ) {

            OutlinedTextField(
                value = number1,
                onValueChange = { number1 = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Number 1") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)), // Red Color
                    shape = RoundedCornerShape(12.dp),

                    onClick = {
                        result = "plus"

                        focus1.requestFocus()

                        keyboard?.show()

                        add = true


                    }
                ) {
                    Text("+", fontSize = 24.sp)
                }

                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)), // Red Color
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        result = "multiply"
                        focus1.requestFocus()

                        multiply = true
                    }
                ) {
                    Text("x", fontSize = 24.sp)
                }


                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF58)), // Red Color
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        result = "minus"
                        focus1.requestFocus()

                        subtract = true
                    }
                ) {
                    Text("-", fontSize = 24.sp)
                }


                Button(
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF58)), // Green Color

                    onClick = {

                        focus1.requestFocus()

                        divide = true
                    }
                ) {
                    Text("/", fontSize = 24.sp)
                }
            }

            OutlinedTextField(
                value = number2,
                onValueChange = { number2 = it },
                enabled = enabler,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus1),
                label = { Text("Number 2") }
            )

// equal to button
            Button(
                onClick = {

                    keyboard?.hide()
                    focusManager.clearFocus()

                    if (add) {
                        val num1 = number1.toDoubleOrNull() ?: 0.0
                        val num2 = number2.toDoubleOrNull() ?: 0.0
                        result = (num1 + num2).toString()
                    }

                    if (subtract) {
                        val num1 = number1.toDoubleOrNull() ?: 0.0
                        val num2 = number2.toDoubleOrNull() ?: 0.0
                        result = (num1 - num2).toString()
                    }
                    if (multiply) {
                        val num1 = number1.toDoubleOrNull() ?: 0.0
                        val num2 = number2.toDoubleOrNull() ?: 0.0
                        result = (num1 * num2).toString()
                    }
                    if (divide) {
                        val num1 = number1.toDoubleOrNull() ?: 0.0
                        val num2 = number2.toDoubleOrNull() ?: 0.0
                        result = (num1 / num2).toString()
                    }
                    divide = false
                    add = false
                    subtract = false
                    multiply = false
                },
                shape = RoundedCornerShape(12.dp),


                modifier = Modifier
                    .size(50.dp, 50.dp)


            ) {

                Text("=", fontSize = 25.sp)
            }

            OutlinedTextField(
                value = result,
                onValueChange = { result = it },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Result") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly


            ) {
                Text("TEXT1", color = Color.Blue)
                Text("TEXT2", color = Color.Red)
                Text("TEXT3", color = Color.Black)
            }

            Button(onClick = {

            }

            ) {
                Text("666")

            }


        }

//Spacer(modifier = Modifier.height(300.dp))
    }


}







@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MaterialTheme {
        CalculatorScreen()
    }
}

