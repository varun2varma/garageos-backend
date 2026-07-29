/*
==========================================================
GarageOS Welcome
==========================================================
*/

document.addEventListener("DOMContentLoaded", () => {

    bindEvents();

});

function bindEvents() {

    document

        .getElementById("registerGarage")

        .addEventListener(

            "click",

            registerGarage

        );

    document

        .getElementById("joinGarage")

        .addEventListener(

            "click",

            joinGarage

        );

    document

        .getElementById("customerPortal")

        .addEventListener(

            "click",

            customerPortal

        );

}

async function registerGarage() {

    await completeOnboarding();

    window.location.href =
        "../garage/register-garage.html";

}

async function joinGarage() {

    await completeOnboarding();

    window.location.href =
        "../garage/join-garage.html";

}

async function customerPortal() {

    await completeOnboarding();

    window.location.href =
        "../customer/customer-home.html";

}

async function completeOnboarding() {

    try {

        await Api.post(

            "/auth/complete-onboarding",

            {}

        );

    }
    catch (e) {

        console.error(e);

    }

}