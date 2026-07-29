/*
==========================================================
GarageOS Register
==========================================================
*/

document.addEventListener("DOMContentLoaded", () => {

    bindEvents();

});

function bindEvents() {

    document
        .getElementById("registerForm")
        .addEventListener("submit", register);

    document
        .getElementById("togglePassword")
        .addEventListener("click", () =>
            togglePassword(
                "password",
                "#togglePassword i"
            ));

    document
        .getElementById("toggleConfirmPassword")
        .addEventListener("click", () =>
            togglePassword(
                "confirmPassword",
                "#toggleConfirmPassword i"
            ));

}

async function register(event) {

    event.preventDefault();

    hideError();

    const request = {

        firstName:
            document
                .getElementById("firstName")
                .value
                .trim(),

        lastName:
            document
                .getElementById("lastName")
                .value
                .trim(),

        mobile:
            document
                .getElementById("mobile")
                .value
                .trim(),

        email:
            document
                .getElementById("email")
                .value
                .trim(),

        username:
            document
                .getElementById("username")
                .value
                .trim(),

        password:
            document
                .getElementById("password")
                .value

    };

    const confirmPassword =
        document
            .getElementById("confirmPassword")
            .value;

    if (!validate(request, confirmPassword)) {

        return;

    }

    setLoading(true);

    try {

        await Api.post(

            "/auth/register",

            request

        );

        window.location.href =
            "login.html?registered=true";

    }
    catch (e) {

        console.error(e);

        showError(

            e.message ||

            "Registration failed."

        );

    }
    finally {

        setLoading(false);

    }

}

function validate(request, confirmPassword) {

    if (

        !request.firstName ||

        !request.lastName ||

        !request.mobile ||

        !request.email ||

        !request.username ||

        !request.password

    ) {

        showError(

            "Please fill all required fields."

        );

        return false;

    }

    if (

        !/^[6-9]\d{9}$/

            .test(request.mobile)

    ) {

        showError(

            "Please enter a valid mobile number."

        );

        return false;

    }

    if (

        request.password.length < 8

    ) {

        showError(

            "Password must contain at least 8 characters."

        );

        return false;

    }

    if (

        request.password !== confirmPassword

    ) {

        showError(

            "Passwords do not match."

        );

        return false;

    }

    return true;

}

function togglePassword(inputId, iconSelector) {

    const input =

        document.getElementById(inputId);

    const icon =

        document.querySelector(iconSelector);

    if (input.type === "password") {

        input.type = "text";

        icon.className =

            "bi bi-eye-slash";

    }
    else {

        input.type = "password";

        icon.className =

            "bi bi-eye";

    }

}

function setLoading(loading) {

    document
        .getElementById("registerButton")
        .disabled = loading;

    document
        .getElementById("registerSpinner")
        .classList
        .toggle("d-none", !loading);

    document
        .getElementById("registerText")
        .textContent =

        loading

            ? "Creating Account..."

            : "CREATE ACCOUNT";

}

function showError(message) {

    const alert =

        document.getElementById(

            "errorMessage"

        );

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