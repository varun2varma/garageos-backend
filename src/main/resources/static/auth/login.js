/*
==========================================================
GarageOS Login
==========================================================
*/

document.addEventListener("DOMContentLoaded", () => {

    bindEvents();

    loadRememberedUsername();

});

function bindEvents() {

    document
        .getElementById("loginForm")
        .addEventListener("submit", login);

    document
        .getElementById("togglePassword")
        .addEventListener("click", togglePassword);

}

async function login(event) {

    event.preventDefault();

    hideError();

    const username =
        document
            .getElementById("username")
            .value
            .trim();

    const password =
        document
            .getElementById("password")
            .value;

    if (!username || !password) {

        showError(
            "Username and Password are required."
        );

        return;
    }

    setLoading(true);

    try {

        const response =
            await Api.post(
                "/auth/login",
                {
                    username,
                    password
                }
            );

        /*
        Expected Response

        {
            accessToken:"",
            refreshToken:"",
            expiresIn:3600,
            user:{}
        }
        */

        Auth.saveSession(response);

        rememberUsername(username);

        if (response.firstLogin) {

            window.location.href =
                "../onboarding/welcome.html";

        }
        else {

            window.location.href =
                "../index.html";

        }

    }
    catch (e) {

        console.error(e);

        showError(

            e.message ||

            "Invalid username or password."

        );

    }
    finally {

        setLoading(false);

    }

}

function togglePassword() {

    const input =
        document.getElementById("password");

    const icon =
        document
            .querySelector("#togglePassword i");

    if (input.type === "password") {

        input.type = "text";

        icon.className = "bi bi-eye-slash";

    }
    else {

        input.type = "password";

        icon.className = "bi bi-eye";

    }

}

function setLoading(loading) {

    document
        .getElementById("loginButton")
        .disabled = loading;

    document
        .getElementById("loginSpinner")
        .classList
        .toggle("d-none", !loading);

    document
        .getElementById("loginText")
        .textContent =
        loading
            ? "Signing In..."
            : "SIGN IN";

}

function showError(message) {

    const alert =
        document.getElementById("errorMessage");

    alert.innerHTML =

        `<i class="bi bi-exclamation-circle-fill"></i>
         ${message}`;

    alert.classList.remove("d-none");

}

function hideError() {

    document
        .getElementById("errorMessage")
        .classList
        .add("d-none");

}

function rememberUsername(username) {

    const remember =
        document
            .getElementById("rememberMe")
            .checked;

    if (remember) {

        localStorage.setItem(
            "remember_username",
            username
        );

    }
    else {

        localStorage.removeItem(
            "remember_username"
        );

    }

}

function loadRememberedUsername() {

    const username =
        localStorage.getItem(
            "remember_username"
        );

    if (!username) {

        return;

    }

    document
        .getElementById("username")
        .value = username;

    document
        .getElementById("rememberMe")
        .checked = true;

}