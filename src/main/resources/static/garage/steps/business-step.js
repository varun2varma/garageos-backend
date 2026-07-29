window.BusinessStep = {

    render(data) {

//        setTimeout(() => this.bindEvents(data), 0);

        return `

            <div class="step-title">

                Business Information

            </div>

            <div class="step-subtitle">

                These details are optional and can be updated later.

            </div>

            <div class="mb-4">

                <label class="form-label">

                    GST Number

                </label>

                <input

                    id="gstNumber"

                    type="text"

                    class="form-control"

                    maxlength="15"

                    placeholder="22AAAAA0000A1Z5"

                    value="${data.gstNumber || ""}">

                <div class="form-text">

                    Optional

                </div>

            </div>

            <div class="mb-4">

                <label class="form-label">

                    PAN Number

                </label>

                <input

                    id="panNumber"

                    type="text"

                    class="form-control"

                    maxlength="10"

                    placeholder="ABCDE1234F"

                    value="${data.panNumber || ""}">

                <div class="form-text">

                    Optional

                </div>

            </div>

        `;

    },

        bindEvents(data) {

            const gstNumber =
                document.getElementById("gstNumber");

            const panNumber =
                document.getElementById("panNumber");

            if (!gstNumber || !panNumber) {
                console.error("Business step elements not found.");
                return;
            }

            gstNumber.addEventListener("input", e => {

                const value =
                    e.target.value
                        .toUpperCase()
                        .replace(/\s/g, "");

                e.target.value = value;

                data.gstNumber = value;

            });

            panNumber.addEventListener("input", e => {

                const value =
                    e.target.value
                        .toUpperCase()
                        .replace(/\s/g, "");

                e.target.value = value;

                data.panNumber = value;

            });

        },

        validate(data) {

            const gst =
                (data.gstNumber || "").trim();

            const pan =
                (data.panNumber || "").trim();

            if (gst) {

                const gstRegex =
                    /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/;

                if (!gstRegex.test(gst)) {

                    alert("Please enter a valid GST Number.");

                    document
                        .getElementById("gstNumber")
                        ?.focus();

                    return false;

                }

            }

            if (pan) {

                const panRegex =
                    /^[A-Z]{5}[0-9]{4}[A-Z]$/;

                if (!panRegex.test(pan)) {

                    alert("Please enter a valid PAN Number.");

                    document
                        .getElementById("panNumber")
                        ?.focus();

                    return false;

                }

            }

            return true;

        }

    };