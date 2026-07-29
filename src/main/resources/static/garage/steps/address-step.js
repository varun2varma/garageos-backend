window.AddressStep = {

    render(data) {

//        setTimeout(() => this.bindEvents(data), 0);

        return `

            <div class="step-title">

                Garage Address

            </div>

            <div class="step-subtitle">

                Where is your garage located?

            </div>

            <div class="mb-4">

                <label class="form-label">

                    Address <span class="text-danger">*</span>

                </label>

                <textarea

                    id="address"

                    class="form-control"

                    rows="3"

                    maxlength="250"

                    placeholder="Door No, Street, Area">${data.address || ""}</textarea>

            </div>

            <div class="mb-4">

                <label class="form-label">

                    Landmark

                </label>

                <input

                    id="landmark"

                    type="text"

                    class="form-control"

                    maxlength="100"

                    placeholder="Nearby landmark"

                    value="${data.landmark || ""}">

            </div>

            <div class="row">

                <div class="col-md-6 mb-4">

                    <label class="form-label">

                        City <span class="text-danger">*</span>

                    </label>

                    <input

                        id="city"

                        type="text"

                        class="form-control"

                        maxlength="100"

                        placeholder="City"

                        value="${data.city || ""}">

                </div>

                <div class="col-md-6 mb-4">

                    <label class="form-label">

                        State <span class="text-danger">*</span>

                    </label>

                    <input

                        id="state"

                        type="text"

                        class="form-control"

                        maxlength="100"

                        placeholder="State"

                        value="${data.state || ""}">

                </div>

            </div>

            <div class="row">

                <div class="col-md-6">

                    <label class="form-label">

                        Pincode <span class="text-danger">*</span>

                    </label>

                    <input

                        id="pincode"

                        type="text"

                        class="form-control"

                        maxlength="6"

                        inputmode="numeric"

                        placeholder="500081"

                        value="${data.pincode || ""}">

                </div>

            </div>

        `;

    },

        bindEvents(data) {

            const address =
                document.getElementById("address");

            const landmark =
                document.getElementById("landmark");

            const city =
                document.getElementById("city");

            const state =
                document.getElementById("state");

            const pincode =
                document.getElementById("pincode");

            address.addEventListener("input", e => {

                data.address = e.target.value;

            });

            landmark.addEventListener("input", e => {

                data.landmark = e.target.value;

            });

            city.addEventListener("input", e => {

                data.city = e.target.value;

            });

            state.addEventListener("input", e => {

                data.state = e.target.value;

            });

            pincode.addEventListener("input", e => {

                let value = e.target.value
                    .replace(/\D/g, "")
                    .substring(0, 6);

                e.target.value = value;

                data.pincode = value;

            });

        },

        validate(data) {

            const address =
                data.address
                    ? data.address.trim()
                    : "";

            if (!address) {

                alert("Please enter the garage address.");

                document
                    .getElementById("address")
                    ?.focus();

                return false;

            }

            if (address.length > 250) {

                alert("Garage address cannot exceed 250 characters.");

                document
                    .getElementById("address")
                    ?.focus();

                return false;

            }

            const city =
                data.city
                    ? data.city.trim()
                    : "";

            if (!city) {

                alert("Please enter the city.");

                document
                    .getElementById("city")
                    ?.focus();

                return false;

            }

            const state =
                data.state
                    ? data.state.trim()
                    : "";

            if (!state) {

                alert("Please enter the state.");

                document
                    .getElementById("state")
                    ?.focus();

                return false;

            }

            const pincode =
                data.pincode
                    ? data.pincode.trim()
                    : "";

            if (!/^\d{6}$/.test(pincode)) {

                alert("Please enter a valid 6-digit pincode.");

                document
                    .getElementById("pincode")
                    ?.focus();

                return false;

            }

            return true;

        }

    };