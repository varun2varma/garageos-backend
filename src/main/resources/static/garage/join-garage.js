document.addEventListener(

    "DOMContentLoaded",

    initialize

);

function initialize() {

    document

        .getElementById(

            "joinGarageButton"

        )

        .addEventListener(

            "click",

            joinGarage

        );

}

async function joinGarage() {

    const garageCode =

        document

            .getElementById(

                "garageCode"

            )

            .value

            .trim()

            .toUpperCase();

    if (!garageCode) {

        showError(

            "Please enter garage code."

        );

        return;

    }

    try {

        await GarageService.joinGarage({

            garageCode

        });

        showSuccess(

            "Your request has been submitted successfully."

        );

        setTimeout(() => {

            window.location.href =

                "pending-approval.html";

        }, 1200);

    }

    catch (e) {

        showError(

            e.message

        );

    }

}

function showSuccess(message) {

    document

        .getElementById(

            "joinMessage"

        )

        .innerHTML =

        `

        <div class="alert alert-success">

            <i class="bi bi-check-circle-fill"></i>

            ${message}

        </div>

        `;

}

function showError(message) {

    document

        .getElementById(

            "joinMessage"

        )

        .innerHTML =

        `

        <div class="alert alert-danger">

            <i class="bi bi-exclamation-circle-fill"></i>

            ${message}

        </div>

        `;

}