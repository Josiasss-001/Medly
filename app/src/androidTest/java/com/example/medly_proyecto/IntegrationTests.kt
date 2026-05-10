package com.example.medly_proyecto

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.medly_proyecto.ui.AuthActivity
import com.example.medly_proyecto.ui.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@LargeTest
class IntegrationTests {

    @Before
    fun configurarEscenario() {
        // Aseguramos un estado limpio cerrando sesión antes de cada prueba
        FirebaseAuth.getInstance().signOut()
    }

    private fun generarEmailAleatorio(): String {
        return "test_user_${UUID.randomUUID().toString().substring(0, 8)}@medly.com"
    }

    @Test
    fun navegarARegistroYVolverALogin() {
        ActivityScenario.launch(AuthActivity::class.java)
        
        // Ir a Registro
        onView(withId(R.id.signupButton)).perform(click())
        
        // Verificar que estamos en la pantalla de Registro
        onView(withId(R.id.confirmPasswordEditText)).check(matches(isDisplayed()))
        
        // Volver al Login
        onView(withId(R.id.volverButton)).perform(click())
        
        // Verificar que regresamos exitosamente
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
    }

    @Test
    fun flujoCompletoRegistroYIngresoManual() {
        val email = generarEmailAleatorio()
        val password = "Password123!"

        ActivityScenario.launch(AuthActivity::class.java)
        
        // 1. Navegar a Registro
        onView(withId(R.id.signupButton)).perform(click())

        // 2. Llenar Formulario de Registro
        onView(withId(R.id.emailEditText)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.passwordEditText)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.confirmPasswordEditText)).perform(typeText(password), closeSoftKeyboard())

        // 3. Registrarse (Guarda en Firestore y hace Logout automático)
        onView(withId(R.id.registerButton)).perform(click())

        // Esperar el proceso de Firebase y la transición de ventana
        Thread.sleep(6000) 

        // 4. Verificar que volvimos al Login y que la ventana tiene foco
        // El botón de login debe ser visible, indicando que NO entramos automáticamente
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))

        // 5. Iniciar Sesión manualmente con la cuenta creada
        onView(withId(R.id.emailEditText)).perform(clearText(), typeText(email), closeSoftKeyboard())
        onView(withId(R.id.passwordEditText)).perform(clearText(), typeText(password), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        // Esperar validación e ingreso al Home
        Thread.sleep(7000)

        // 6. Verificar que finalmente llegamos al Home
        onView(withId(R.id.userNameTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun validacionDeCamposVaciosEnRegistro() {
        ActivityScenario.launch(AuthActivity::class.java)
        onView(withId(R.id.signupButton)).perform(click())
        
        // Intentar registro sin datos
        onView(withId(R.id.registerButton)).perform(click())
        
        // Verificar que el botón de registro sigue ahí (no cambió de pantalla)
        onView(withId(R.id.registerButton)).check(matches(isDisplayed()))
    }

    @Test
    fun navegacionMenuLateralYVerificarItems() {
        // Lanzamos Home directamente para probar el Drawer
        ActivityScenario.launch(HomeActivity::class.java)
        
        onView(withId(R.id.menuIcon)).perform(click())
        onView(withId(R.id.navigationView)).check(matches(isDisplayed()))

        // Buscamos "Recetas" dentro del Drawer para evitar conflictos con el menú inferior
        onView(allOf(withText(R.string.recetas), isDescendantOfA(withId(R.id.navigationView))))
            .check(matches(isDisplayed()))
    }

    @Test
    fun flujoCerrarSesionDesdeElHome() {
        ActivityScenario.launch(HomeActivity::class.java)
        
        onView(withId(R.id.menuIcon)).perform(click())
        
        // Click en Cerrar Sesión
        onView(allOf(withText(R.string.cerrar_sesion), isDescendantOfA(withId(R.id.navigationView))))
            .perform(click())

        // Verificar redirección al Login
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
    }
}
