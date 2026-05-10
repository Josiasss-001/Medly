package com.example.medly_proyecto

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.medly_proyecto.ui.AuthActivity
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class PruebaAutenticacion {

    @Before
    fun prepararEntorno() {
        FirebaseAuth.getInstance().signOut()
        IdlingPolicies.setMasterPolicyTimeout(30, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(30, TimeUnit.SECONDS)
    }

    private fun generarEmailAleatorio() = "user_${UUID.randomUUID().toString().substring(0, 8)}@medlytest.com"

    @Test
    fun verificarFlujoRegistroEIngresoConPasos() {
        val email = generarEmailAleatorio()
        val password = "Password123!"

        // 1. Iniciar Auth
        ActivityScenario.launch(AuthActivity::class.java)

        // 2. Registro
        onView(withId(R.id.signupButton)).perform(click())
        onView(withId(R.id.emailEditText)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.passwordEditText)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.confirmPasswordEditText)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.registerButton)).perform(click())

        Thread.sleep(7000)

        // 3. Login Manual
        onView(withId(R.id.emailEditText)).perform(clearText(), typeText(email), closeSoftKeyboard())
        onView(withId(R.id.passwordEditText)).perform(clearText(), typeText(password), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        // --- FLUJO DEL RECOLECTOR POR PASOS ---
        Thread.sleep(7000)

        // PASO 1
        onView(withId(R.id.nameEditText)).perform(typeText("Juan"), closeSoftKeyboard())
        onView(withId(R.id.lastNameEditText)).perform(typeText("Pérez"), closeSoftKeyboard())
        onView(withId(R.id.ageEditText)).perform(typeText("30"), closeSoftKeyboard())
        onView(withId(R.id.cityEditText)).perform(typeText("Santiago"), closeSoftKeyboard())
        onView(withId(R.id.nextButton)).perform(click())

        // PASO 2
        onView(withId(R.id.weightEditText)).perform(typeText("75"), closeSoftKeyboard())
        onView(withId(R.id.heightEditText)).perform(typeText("180"), closeSoftKeyboard())
        onView(withId(R.id.birthDateEditText)).perform(replaceText("10/10/1994"), closeSoftKeyboard())
        onView(withId(R.id.nextButton)).perform(click())

        // PASO 3
        onView(withId(R.id.radioNo)).perform(click())
        onView(withId(R.id.saveButton)).perform(click())

        Thread.sleep(8000)

        // 4. Verificar Home
        onView(withId(R.id.userNameTextView)).check(matches(isDisplayed()))
    }
}
