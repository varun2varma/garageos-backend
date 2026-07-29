window.GarageStep = {

    render(data) {

//        setTimeout(() => this.bindEvents(data), 0);

        return `

            <div class="step-title">

                Garage Information

            </div>

            <div class="step-subtitle">

                Tell us about your garage.

            </div>

            <div class="mb-4">

                <label class="form-label">

                    Garage Name <span class="text-danger">*</span>

                </label>

                <input

                    id="garageName"

                    type="text"

                    class="form-control"

                    maxlength="100"

                    placeholder="Enter garage name"

                    value="${data.garageName || ""}">

            </div>

            <div class="mb-4">

                <label class="form-label">

                    Number Of Service Bays <span class="text-danger">*</span>

                </label>

                <input

                    id="numberOfBays"

                    type="number"

                    min="1"

                    max="100"

                    class="form-control"

                    placeholder="Example: 4"

                    value="${data.numberOfBays || ""}">

            </div>

            <div class="mb-3">

                <label class="form-label">

                    Workshop Type <span class="text-danger">*</span>

                </label>

            </div>

            <div class="workshop-grid">

                ${this.card(
                    "MULTI_BRAND",
                    "🚗",
                    "Multi Brand",
                    "Services all vehicle brands",
                    data.workshopType
                )}

                ${this.card(
                    "GENERAL_SERVICE",
                    "🔧",
                    "General Service",
                    "General repair and maintenance",
                    data.workshopType
                )}

                ${this.card(
                    "BODY_SHOP",
                    "🎨",
                    "Body Shop",
                    "Denting, painting and accident repairs",
                    data.workshopType
                )}

                ${this.card(
                    "TYRE_SHOP",
                    "🛞",
                    "Tyre Shop",
                    "Tyres, balancing and alignment",
                    data.workshopType
                )}

            </div>

        `;

    },

    card(value, icon, title, description, selected) {

        return `

            <div

                class="workshop-card ${selected === value ? "selected" : ""}"

                data-value="${value}">

                <div class="workshop-icon">

                    ${icon}

                </div>

                <div class="workshop-title">

                    ${title}

                </div>

                <div class="workshop-description">

                    ${description}

                </div>

            </div>

        `;

    },

        bindEvents(data) {

            const garageName = document.getElementById("garageName");
            const numberOfBays = document.getElementById("numberOfBays");

            if (!garageName || !numberOfBays) {
                console.error("Garage step elements not found.");
                return;
            }

            garageName.addEventListener("input", e => {

                data.garageName = e.target.value;

            });

            numberOfBays.addEventListener("input", e => {

                data.numberOfBays = e.target.value;

            });

            document

                .querySelectorAll(".workshop-card")

                .forEach(card => {

                    card.addEventListener("click", () => {

                        document

                            .querySelectorAll(".workshop-card")

                            .forEach(c =>

                                c.classList.remove("selected")

                            );

                        card.classList.add("selected");

                        data.workshopType =
                            card.dataset.value;

                    });

                });

        },

        validate(data) {

            const garageName =
                data.garageName
                    ? data.garageName.trim()
                    : "";

            if (!garageName) {

                alert("Please enter the garage name.");

                document

                    .getElementById("garageName")

                    ?.focus();

                return false;

            }

            if (garageName.length > 100) {

                alert("Garage name cannot exceed 100 characters.");

                document

                    .getElementById("garageName")

                    ?.focus();

                return false;

            }

            const numberOfBays =
                Number(data.numberOfBays);

            if (

                Number.isNaN(numberOfBays) ||

                numberOfBays < 1

            ) {

                alert("Please enter a valid number of service bays.");

                document

                    .getElementById("numberOfBays")

                    ?.focus();

                return false;

            }

            if (numberOfBays > 100) {

                alert("Number of service bays cannot exceed 100.");

                document

                    .getElementById("numberOfBays")

                    ?.focus();

                return false;

            }

            if (!data.workshopType) {

                alert("Please select a workshop type.");

                return false;

            }

            return true;

        }

    };