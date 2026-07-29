window.ReviewStep = {

    render(data) {

        return `

            <div class="step-title">

                Review & Create

            </div>

            <div class="step-subtitle">

                Please review your garage information before creating your garage.

            </div>

            <div class="review-card">

                <h5>

                    <i class="bi bi-building"></i>

                    Garage Information

                </h5>

                <div class="review-item">

                    <span class="review-label">

                        Garage Name

                    </span>

                    <span class="review-value">

                        ${this.value(data.garageName)}

                    </span>

                </div>

                <div class="review-item">

                    <span class="review-label">

                        Workshop Type

                    </span>

                    <span class="review-value">

                        ${this.workshopType(data.workshopType)}

                    </span>

                </div>

                <div class="review-item">

                    <span class="review-label">

                        Service Bays

                    </span>

                    <span class="review-value">

                        ${this.value(data.numberOfBays)}

                    </span>

                </div>

            </div>

            <div class="review-card">

                <h5>

                    <i class="bi bi-geo-alt"></i>

                    Address

                </h5>

                <div class="review-item">

                    <span class="review-label">

                        Address

                    </span>

                    <span class="review-value">

                        ${this.value(data.address)}

                    </span>

                </div>

                <div class="review-item">

                    <span class="review-label">

                        Landmark

                    </span>

                    <span class="review-value">

                        ${this.value(data.landmark)}

                    </span>

                </div>

                <div class="review-item">

                    <span class="review-label">

                        City

                    </span>

                    <span class="review-value">

                        ${this.value(data.city)}

                    </span>

                </div>

                <div class="review-item">

                    <span class="review-label">

                        State

                    </span>

                    <span class="review-value">

                        ${this.value(data.state)}

                    </span>

                </div>

                <div class="review-item">

                    <span class="review-label">

                        Pincode

                    </span>

                    <span class="review-value">

                        ${this.value(data.pincode)}

                    </span>

                </div>

            </div>

            <div class="review-card">

                <h5>

                    <i class="bi bi-receipt"></i>

                    Business Registration

                </h5>

                <div class="review-item">

                    <span class="review-label">

                        GST Number

                    </span>

                    <span class="review-value">

                        ${this.value(data.gstNumber)}

                    </span>

                </div>

                <div class="review-item">

                    <span class="review-label">

                        PAN Number

                    </span>

                    <span class="review-value">

                        ${this.value(data.panNumber)}

                    </span>

                </div>

            </div>

        `;

    },

    value(value) {

        if (!value || value.trim() === "") {

            return "-";

        }

        return value;

    },

    workshopType(type) {

        switch (type) {

            case "MULTI_BRAND":

                return "Multi Brand";

            case "GENERAL_SERVICE":

                return "General Service";

            case "BODY_SHOP":

                return "Body Shop";

            case "TYRE_SHOP":

                return "Tyre Shop";

            default:

                return "-";

        }

    }

};