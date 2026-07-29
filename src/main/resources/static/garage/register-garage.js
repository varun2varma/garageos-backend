window.GarageRegistration = {

    currentStep: 1,

    totalSteps: 4,

    data: {

        garageName: "",

        workshopType: "",

        numberOfBays: "",

        address: "",

        landmark: "",

        city: "",

        state: "",

        pincode: "",

        gstNumber: "",

        panNumber: ""

    },

    init() {

        this.bindEvents();

        this.renderStep();

    },

    bindEvents() {

        document

            .getElementById("wizardNextButton")

            .addEventListener(

                "click",

                () => this.next()

            );

        document

            .getElementById("wizardBackButton")

            .addEventListener(

                "click",

                () => this.back()

            );

    },

    next() {

        if (!this.validateCurrentStep()) {

            return;

        }

        if (this.currentStep === this.totalSteps) {

            this.submit();

            return;

        }

        this.currentStep++;

        this.renderStep();

    },

    back() {

        if (this.currentStep === 1) {

            return;

        }

        this.currentStep--;

        this.renderStep();

    },

    renderStep() {

        const container =
            document.getElementById("wizardContainer");

        switch (this.currentStep) {

                case 1:

                    container.innerHTML =
                        GarageStep.render(this.data);

                    GarageStep.bindEvents(this.data);

                    break;

                case 2:

                    container.innerHTML =
                        AddressStep.render(this.data);

                    AddressStep.bindEvents(this.data);

                    break;

                case 3:

                    container.innerHTML =
                        BusinessStep.render(this.data);

                    BusinessStep.bindEvents(this.data);

                    break;

                case 4:

                    container.innerHTML =
                        ReviewStep.render(this.data);

                    break;
        }

        this.updateProgress();

        this.updateButtons();

    },

        updateProgress() {

            document.getElementById("currentStep").textContent =
                this.currentStep;

            document.getElementById("totalSteps").textContent =
                this.totalSteps;

            const percentage =
                (this.currentStep / this.totalSteps) * 100;

            document.getElementById("progressBar").style.width =
                percentage + "%";

        },

        updateButtons() {

            const backButton =
                document.getElementById("wizardBackButton");

            const buttonText =
                document.getElementById("wizardButtonText");

            const buttonIcon =
                document.getElementById("wizardButtonIcon");

            backButton.style.visibility =
                this.currentStep === 1
                    ? "hidden"
                    : "visible";

            if (this.currentStep === this.totalSteps) {

                buttonText.textContent =
                    "Create Garage";

                buttonIcon.className =
                    "bi bi-check-circle ms-2";

            } else {

                buttonText.textContent =
                    "Next";

                buttonIcon.className =
                    "bi bi-arrow-right ms-2";

            }

        },

        validateCurrentStep() {

            switch (this.currentStep) {

                case 1:

                    return GarageStep.validate(this.data);

                case 2:

                    return AddressStep.validate(this.data);

                case 3:

                    return BusinessStep.validate(this.data);

                case 4:

                    return true;

                default:

                    return true;

            }

        },

        showError(message) {

            const error =
                document.getElementById("errorMessage");

            error.textContent = message;

            error.classList.remove("d-none");

        },

        hideError() {

            document

                .getElementById("errorMessage")

                .classList

                .add("d-none");

        },

        setLoading(loading) {

            const spinner =
                document.getElementById("wizardSpinner");

            const button =
                document.getElementById("wizardNextButton");

            if (loading) {

                spinner.classList.remove("d-none");

                button.disabled = true;

            } else {

                spinner.classList.add("d-none");

                button.disabled = false;

            }

        },

        async submit() {

            try {

                this.hideError();

                this.setLoading(true);

                const response =
                    await GarageService.createGarage(this.data);

                window.location.href =
                    "../index.html";

            } catch (e) {

                console.error(e);

                this.showError(

                    e.message ||

                    "Unable to create garage."

                );

            } finally {

                this.setLoading(false);

            }

        }

    };

    document.addEventListener(

        "DOMContentLoaded",

        () => GarageRegistration.init()

    );